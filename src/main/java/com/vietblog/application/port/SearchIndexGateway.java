package com.vietblog.application.port;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for spatial (PostGIS) and vector (pgvector) search acceleration.
 * Implementations return {@link Optional#empty()} when database extensions are inactive
 * so the application transparently falls back to pure-Java domain evaluation.
 */
public interface SearchIndexGateway {
    SearchIndexGateway NOOP = new SearchIndexGateway() {};

    default Optional<List<String>> findNearbyCandidateIds(double latitude, double longitude, double radiusKm, int limit) {
        return Optional.empty();
    }

    default Optional<List<String>> findSemanticCandidateIds(double[] queryVector, int limit) {
        return Optional.empty();
    }

    default Optional<List<String>> findImageCandidateIds(double[] queryVector, int limit) {
        return Optional.empty();
    }

    default void upsertPostTextVector(String postId, double[] vector) {}

    default void upsertPostImageVector(String postId, double[] vector) {}

    default void deletePostVectors(String postId) {}
}

