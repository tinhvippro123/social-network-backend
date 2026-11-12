package com.vietblog.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

/**
 * Fast non-blocking probe checking whether the configured Redis server is reachable,
 * avoiding 2-second connection timeouts or noisy background listener errors when Redis is offline.
 */
@Component
public class RedisAvailabilityChecker implements BooleanSupplier {
    private static final Logger log = LoggerFactory.getLogger(RedisAvailabilityChecker.class);
    private static final long CHECK_INTERVAL_MS = 10_000L;

    private final String host;
    private final int port;
    private final AtomicBoolean lastAvailable = new AtomicBoolean(false);
    private final AtomicLong nextCheckAt = new AtomicLong(0L);

    public RedisAvailabilityChecker(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean getAsBoolean() {
        long now = System.currentTimeMillis();
        long scheduled = nextCheckAt.get();
        if (now < scheduled) {
            return lastAvailable.get();
        }
        if (!nextCheckAt.compareAndSet(scheduled, now + CHECK_INTERVAL_MS)) {
            return lastAvailable.get();
        }
        boolean reachable = probeTcp();
        boolean previous = lastAvailable.getAndSet(reachable);
        if (reachable && !previous) {
            log.info("Redis 7+ detected at {}:{} — Redis Cache & Chat Pub/Sub active.", host, port);
        }
        return reachable;
    }

    private boolean probeTcp() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 150);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

