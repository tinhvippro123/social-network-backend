package com.vietblog.infrastructure.search;

import com.vietblog.application.port.SearchIndexGateway;
import com.vietblog.application.service.SearchApplicationService;
import com.vietblog.domain.entity.Category;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.PostImageFeatures;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.search.ImageAlgorithms;
import com.vietblog.domain.search.TextAlgorithms;
import com.vietblog.infrastructure.repository.BookmarkRepository;
import com.vietblog.infrastructure.repository.PostImageFeaturesRepository;
import com.vietblog.infrastructure.repository.SearchCommentRepository;
import com.vietblog.infrastructure.repository.SearchPostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgresSearchGatewayTest {

    @Test
    void formatsPgvectorLiteralsDeterministically() {
        String literal = PostgresSearchGateway.toVectorLiteral(new double[]{0.5, -0.25, 0.0, Double.NaN});
        assertThat(literal).isEqualTo("[0.5,-0.25,0.0,0.0]");
    }

    @Test
    void disablesPostgisAndPgvectorGracefullyOnNonPostgresDataSource() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(meta);
        when(meta.getDatabaseProductName()).thenReturn("H2");

        PostgresSearchGateway gateway = new PostgresSearchGateway(dataSource);
        gateway.initializeExtensions();

        assertThat(gateway.isPostgisEnabled()).isFalse();
        assertThat(gateway.isPgvectorEnabled()).isFalse();
        assertThat(gateway.findNearbyCandidateIds(10.77, 106.70, 5.0, 10)).isEmpty();
        assertThat(gateway.findSemanticCandidateIds(new double[TextAlgorithms.TEXT_VECTOR_DIMENSIONS], 10)).isEmpty();
        assertThat(gateway.findImageCandidateIds(new double[ImageAlgorithms.DIMENSIONS], 10)).isEmpty();
    }

    @Test
    void searchServiceDelegatesToPostgisAndHaversineWhenPostgisCandidatesPresent() {
        SearchPostRepository posts = mock(SearchPostRepository.class);
        PostImageFeaturesRepository images = mock(PostImageFeaturesRepository.class);
        SearchCommentRepository comments = mock(SearchCommentRepository.class);
        BookmarkRepository bookmarks = mock(BookmarkRepository.class);
        SearchFileReader files = mock(SearchFileReader.class);
        SearchIndexGateway indexGateway = mock(SearchIndexGateway.class);

        Post nearPost = samplePost("near", "Quán cà phê Quận 1", "Không gian học tập yên tĩnh", 10.7769, 106.7009);
        Post borderOutPost = samplePost("too-far", "Địa điểm xa", "Nằm ngoài bán kính Haversine", 10.9000, 106.9000);

        when(indexGateway.findNearbyCandidateIds(eq(10.7760), eq(106.7000), eq(5.0), anyInt()))
                .thenReturn(Optional.of(List.of("near", "too-far")));
        when(posts.findPublishedByIdIn(List.of("near", "too-far"))).thenReturn(List.of(nearPost, borderOutPost));

        SearchApplicationService service = new SearchApplicationService(posts, images, comments, bookmarks, files, indexGateway);
        var results = service.nearby(10.7760, 106.7000, 5.0, 10);

        assertThat(results).extracting("id").containsExactly("near");
        verify(indexGateway).findNearbyCandidateIds(eq(10.7760), eq(106.7000), eq(5.0), anyInt());
        verify(posts, never()).findByStatusOrderByCreatedAtDescIdAsc(any(), any());
    }

    @Test
    void searchServiceUsesPgvectorCandidatesThenRanksWithTfIdfAndImageCosine() {
        SearchPostRepository posts = mock(SearchPostRepository.class);
        PostImageFeaturesRepository images = mock(PostImageFeaturesRepository.class);
        SearchCommentRepository comments = mock(SearchCommentRepository.class);
        BookmarkRepository bookmarks = mock(BookmarkRepository.class);
        SearchFileReader files = mock(SearchFileReader.class);
        SearchIndexGateway indexGateway = mock(SearchIndexGateway.class);

        Post bestPost = samplePost("p1", "Spring Boot PostgreSQL PostGIS", "Hướng dẫn tích hợp PostGIS và pgvector trong Java Spring Boot", 10.77, 106.70);
        Post weakerPost = samplePost("p2", "Giới thiệu Java cơ bản", "Ngôn ngữ lập trình hướng đối tượng Java", 10.77, 106.70);

        when(indexGateway.findSemanticCandidateIds(any(double[].class), anyInt()))
                .thenReturn(Optional.of(List.of("p2", "p1")));
        when(posts.findPublishedByIdIn(List.of("p2", "p1"))).thenReturn(List.of(weakerPost, bestPost));

        SearchApplicationService service = new SearchApplicationService(posts, images, comments, bookmarks, files, indexGateway);
        var semanticResults = service.search("Spring Boot PostGIS pgvector", "semantic", 5);
        assertThat(semanticResults).extracting("id").containsExactly("p1");

        // Image pgvector candidate + Cosine verification
        BufferedImage redImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) redImage.setRGB(x, y, Color.RED.getRGB());
        double[] redVec = ImageAlgorithms.features(redImage);
        String encodedRed = Arrays.stream(redVec).mapToObj(Double::toString).collect(Collectors.joining(","));

        MultipartFile mockUpload = mock(MultipartFile.class);
        when(files.image(mockUpload)).thenReturn(redImage);
        when(indexGateway.findImageCandidateIds(any(double[].class), anyInt()))
                .thenReturn(Optional.of(List.of("p1")));
        when(posts.findPublishedByIdIn(List.of("p1"))).thenReturn(List.of(bestPost));
        PostImageFeatures stored = new PostImageFeatures();
        stored.setPostId("p1");
        stored.setPost(bestPost);
        stored.setFeatures(encodedRed);
        when(images.findByPostIdIn(any())).thenReturn(List.of(stored));

        var imageResults = service.searchImage(mockUpload, 5);
        assertThat(imageResults).extracting("id").containsExactly("p1");
    }

    private static Post samplePost(String id, String title, String content, Double lat, Double lng) {
        return Post.builder()
                .id(id)
                .title(title)
                .content(content)
                .tags(List.of("java"))
                .status(Post.PostStatus.PUBLISHED)
                .author(User.builder().id("u1").name("Tác giả").build())
                .category(Category.builder().id("c1").name("Công nghệ").slug("cong-nghe").build())
                .latitude(lat)
                .longitude(lng)
                .build();
    }
}

