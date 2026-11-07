package com.vietblog.infrastructure.search;

import com.vietblog.application.port.SearchIndexGateway;
import com.vietblog.domain.search.ImageAlgorithms;
import com.vietblog.domain.search.TextAlgorithms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * PostgreSQL spatial (PostGIS: ST_DWithin, ST_Distance) and vector (pgvector: <=> cosine distance)
 * search index adapter. Automatically detects available PostgreSQL extensions at startup and
 * falls back cleanly when running on H2 or PostgreSQL instances without optional extensions.
 */
@Component
public class PostgresSearchGateway implements SearchIndexGateway {
    private static final Logger log = LoggerFactory.getLogger(PostgresSearchGateway.class);

    private final DataSource dataSource;
    private volatile boolean postgisEnabled = false;
    private volatile boolean pgvectorEnabled = false;

    public PostgresSearchGateway(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isPostgisEnabled() {
        return postgisEnabled;
    }

    public boolean isPgvectorEnabled() {
        return pgvectorEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    public void initializeExtensions() {
        try (Connection conn = dataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase(Locale.ROOT).contains("postgresql")) {
                postgisEnabled = false;
                pgvectorEnabled = false;
                return;
            }

            Set<String> available = queryAvailableExtensions(conn);
            if (available.contains("postgis")) {
                setupPostgis(conn);
            }
            if (available.contains("vector")) {
                setupPgvector(conn);
            }
        } catch (SQLException ex) {
            log.warn("Skipping PostgreSQL spatial/vector extension initialization: {}", ex.getMessage());
            postgisEnabled = false;
            pgvectorEnabled = false;
        }
    }

    private Set<String> queryAvailableExtensions(Connection conn) throws SQLException {
        Set<String> names = new HashSet<>();
        String sql = "SELECT name FROM pg_available_extensions WHERE name IN ('postgis', 'vector')";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }
        return names;
    }

    private void setupPostgis(Connection conn) {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE EXTENSION IF NOT EXISTS postgis");
            st.execute("ALTER TABLE posts ADD COLUMN IF NOT EXISTS geom geography(Point, 4326)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_posts_geom ON posts USING GIST (geom)");
            st.execute("""
                    CREATE OR REPLACE FUNCTION vietblog_sync_post_geom() RETURNS trigger AS $$
                    BEGIN
                        IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
                            NEW.geom := ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326)::geography;
                        ELSE
                            NEW.geom := NULL;
                        END IF;
                        RETURN NEW;
                    END;
                    $$ LANGUAGE plpgsql
                    """);
            st.execute("DROP TRIGGER IF EXISTS trg_posts_sync_geom ON posts");
            st.execute("""
                    CREATE TRIGGER trg_posts_sync_geom
                    BEFORE INSERT OR UPDATE OF latitude, longitude ON posts
                    FOR EACH ROW EXECUTE FUNCTION vietblog_sync_post_geom()
                    """);
            st.execute("""
                    UPDATE posts
                    SET geom = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
                    WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND geom IS NULL
                    """);
            postgisEnabled = true;
            log.info("PostGIS spatial search enabled (ST_DWithin + ST_Distance + GIST index).");
        } catch (SQLException ex) {
            postgisEnabled = false;
            log.warn("PostGIS available but could not be initialized; using pure-Java Haversine fallback: {}", ex.getMessage());
        }
    }

    private void setupPgvector(Connection conn) {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE EXTENSION IF NOT EXISTS vector");
            st.execute("CREATE TABLE IF NOT EXISTS post_text_vectors ("
                    + "post_id VARCHAR(255) PRIMARY KEY REFERENCES posts(id) ON DELETE CASCADE, "
                    + "embedding vector(" + TextAlgorithms.TEXT_VECTOR_DIMENSIONS + ") NOT NULL)");
            st.execute("ALTER TABLE post_image_features ADD COLUMN IF NOT EXISTS embedding vector(" + ImageAlgorithms.DIMENSIONS + ")");
            st.execute("""
                    UPDATE post_image_features
                    SET embedding = CAST('[' || features || ']' AS vector)
                    WHERE embedding IS NULL AND features IS NOT NULL AND features <> ''
                    """);
            backfillMissingTextVectors(conn);
            pgvectorEnabled = true;
            log.info("pgvector similarity search enabled ({}-d TF-IDF & {}-d CBIR embeddings).",
                    TextAlgorithms.TEXT_VECTOR_DIMENSIONS, ImageAlgorithms.DIMENSIONS);
        } catch (SQLException ex) {
            pgvectorEnabled = false;
            log.warn("pgvector available but could not be initialized; using pure-Java Cosine fallback: {}", ex.getMessage());
        }
    }

    private void backfillMissingTextVectors(Connection conn) throws SQLException {
        String selectSql = """
                SELECT p.id, p.title, p.excerpt, p.content
                FROM posts p
                LEFT JOIN post_text_vectors v ON v.post_id = p.id
                WHERE p.status = 'PUBLISHED' AND v.post_id IS NULL
                """;
        String upsertSql = """
                INSERT INTO post_text_vectors (post_id, embedding)
                VALUES (?, CAST(? AS vector))
                ON CONFLICT (post_id) DO UPDATE SET embedding = EXCLUDED.embedding
                """;
        try (PreparedStatement selectPs = conn.prepareStatement(selectSql);
             ResultSet rs = selectPs.executeQuery();
             PreparedStatement upsertPs = conn.prepareStatement(upsertSql)) {
            int count = 0;
            while (rs.next()) {
                String postId = rs.getString("id");
                String title = Objects.toString(rs.getString("title"), "");
                String excerpt = Objects.toString(rs.getString("excerpt"), "");
                String content = Objects.toString(rs.getString("content"), "");
                String combined = title + " " + title + " " + excerpt + " " + content;
                if (combined.length() > 20_000) {
                    combined = combined.substring(0, 20_000);
                }
                double[] vector = TextAlgorithms.hashedTfIdfVector(combined);
                if (isZeroVector(vector)) continue;
                upsertPs.setString(1, postId);
                upsertPs.setString(2, toVectorLiteral(vector));
                upsertPs.addBatch();
                if (++count % 100 == 0) {
                    upsertPs.executeBatch();
                }
            }
            if (count % 100 != 0) {
                upsertPs.executeBatch();
            }
        }
    }

    @Override
    public Optional<List<String>> findNearbyCandidateIds(double latitude, double longitude, double radiusKm, int limit) {
        if (!postgisEnabled) {
            return Optional.empty();
        }
        String sql = """
                SELECT p.id,
                       ST_Distance(p.geom, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography) AS dist_meters
                FROM posts p
                WHERE p.status = 'PUBLISHED'
                  AND p.geom IS NOT NULL
                  AND ST_DWithin(p.geom, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?)
                ORDER BY dist_meters ASC, p.id ASC
                LIMIT ?
                """;
        double radiusMetersWithMargin = radiusKm * 1000.0 + 100.0;
        int candidateLimit = Math.max(limit * 3, 50);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, longitude);
            ps.setDouble(2, latitude);
            ps.setDouble(3, longitude);
            ps.setDouble(4, latitude);
            ps.setDouble(5, radiusMetersWithMargin);
            ps.setInt(6, candidateLimit);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getString(1));
                }
                return Optional.of(ids);
            }
        } catch (SQLException ex) {
            log.warn("PostGIS nearby query failed; falling back to pure-Java Haversine: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<String>> findSemanticCandidateIds(double[] queryVector, int limit) {
        if (!pgvectorEnabled || isZeroVector(queryVector)) {
            return Optional.empty();
        }
        String sql = """
                SELECT v.post_id
                FROM post_text_vectors v
                JOIN posts p ON p.id = v.post_id
                WHERE p.status = 'PUBLISHED'
                ORDER BY v.embedding <=> CAST(? AS vector) ASC, v.post_id ASC
                LIMIT ?
                """;
        String literal = toVectorLiteral(queryVector);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, literal);
            ps.setInt(2, Math.max(limit, 50));
            try (ResultSet rs = ps.executeQuery()) {
                List<String> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getString(1));
                }
                return ids.isEmpty() ? Optional.empty() : Optional.of(ids);
            }
        } catch (SQLException ex) {
            log.warn("pgvector semantic query failed; falling back to pure-Java TF-IDF scan: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<String>> findImageCandidateIds(double[] queryVector, int limit) {
        if (!pgvectorEnabled || isZeroVector(queryVector)) {
            return Optional.empty();
        }
        String sql = """
                SELECT f.post_id
                FROM post_image_features f
                JOIN posts p ON p.id = f.post_id
                WHERE p.status = 'PUBLISHED'
                  AND f.embedding IS NOT NULL
                ORDER BY f.embedding <=> CAST(? AS vector) ASC, f.post_id ASC
                LIMIT ?
                """;
        String literal = toVectorLiteral(queryVector);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, literal);
            ps.setInt(2, Math.max(limit, 50));
            try (ResultSet rs = ps.executeQuery()) {
                List<String> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getString(1));
                }
                return ids.isEmpty() ? Optional.empty() : Optional.of(ids);
            }
        } catch (SQLException ex) {
            log.warn("pgvector image query failed; falling back to pure-Java CBIR scan: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void upsertPostTextVector(String postId, double[] vector) {
        if (!pgvectorEnabled || postId == null || isZeroVector(vector)) {
            return;
        }
        String sql = """
                INSERT INTO post_text_vectors (post_id, embedding)
                VALUES (?, CAST(? AS vector))
                ON CONFLICT (post_id) DO UPDATE SET embedding = EXCLUDED.embedding
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, postId);
            ps.setString(2, toVectorLiteral(vector));
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.warn("Could not upsert pgvector text embedding for post {}: {}", postId, ex.getMessage());
        }
    }

    @Override
    public void upsertPostImageVector(String postId, double[] vector) {
        if (!pgvectorEnabled || postId == null || isZeroVector(vector)) {
            return;
        }
        String sql = "UPDATE post_image_features SET embedding = CAST(? AS vector) WHERE post_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, toVectorLiteral(vector));
            ps.setString(2, postId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.warn("Could not upsert pgvector image embedding for post {}: {}", postId, ex.getMessage());
        }
    }

    @Override
    public void deletePostVectors(String postId) {
        if (!pgvectorEnabled || postId == null) {
            return;
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM post_text_vectors WHERE post_id = ?")) {
            ps.setString(1, postId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.warn("Could not delete pgvector text embedding for post {}: {}", postId, ex.getMessage());
        }
    }

    public static String toVectorLiteral(double[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(vector.length * 10);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            double val = vector[i];
            sb.append(Double.isFinite(val) ? val : 0.0);
        }
        sb.append(']');
        return sb.toString();
    }

    private static boolean isZeroVector(double[] vector) {
        if (vector == null || vector.length == 0) return true;
        for (double v : vector) {
            if (Double.isFinite(v) && v != 0.0) return false;
        }
        return true;
    }
}

