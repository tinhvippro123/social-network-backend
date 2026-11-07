package com.vietblog.infrastructure.cache;

import com.vietblog.application.port.CacheGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis-backed distributed cache with a short circuit-breaker cooldown so the application
 * runs at full speed even when a local Redis server is not started.
 */
@Component
public class ResilientRedisCacheGateway implements CacheGateway {
    private static final Logger log = LoggerFactory.getLogger(ResilientRedisCacheGateway.class);
    private static final long COOLDOWN_MS = 10_000L;

    private final StringRedisTemplate redis;
    private final AtomicLong circuitOpenUntil = new AtomicLong(0L);

    public ResilientRedisCacheGateway(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<String> get(String key) {
        if (key == null || isCircuitOpen()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(redis.opsForValue().get(key));
        } catch (RuntimeException ex) {
            tripCircuit(ex);
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, String jsonValue, Duration ttl) {
        if (key == null || jsonValue == null || ttl == null || isCircuitOpen()) {
            return;
        }
        try {
            redis.opsForValue().set(key, jsonValue, ttl);
        } catch (RuntimeException ex) {
            tripCircuit(ex);
        }
    }

    @Override
    public void evict(String key) {
        if (key == null || isCircuitOpen()) {
            return;
        }
        try {
            redis.delete(key);
        } catch (RuntimeException ex) {
            tripCircuit(ex);
        }
    }

    private boolean isCircuitOpen() {
        return System.currentTimeMillis() < circuitOpenUntil.get();
    }

    private void tripCircuit(RuntimeException ex) {
        long now = System.currentTimeMillis();
        long prev = circuitOpenUntil.getAndSet(now + COOLDOWN_MS);
        if (now >= prev) {
            log.debug("Redis cache unavailable ({}); bypassing cache for {}ms.", ex.getMessage(), COOLDOWN_MS);
        }
    }
}

