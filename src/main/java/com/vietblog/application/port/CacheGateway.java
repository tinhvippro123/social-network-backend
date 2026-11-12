package com.vietblog.application.port;

import java.time.Duration;
import java.util.Optional;

/**
 * Outbound port for distributed caching (Redis).
 * Implementations fail open so cache outages never interrupt user requests.
 */
public interface CacheGateway {
    CacheGateway NOOP = new CacheGateway() {};

    default Optional<String> get(String key) {
        return Optional.empty();
    }

    default void put(String key, String jsonValue, Duration ttl) {}

    default void evict(String key) {}
}

