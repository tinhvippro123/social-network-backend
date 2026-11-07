package com.vietblog.infrastructure.messaging;

import com.vietblog.application.service.ChatEvent;
import com.vietblog.infrastructure.cache.RedisAvailabilityChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;

@Component
public class ChatEventRelay {
    public static final String CHANNEL = "vietblog.chat.events";
    private static final Logger log = LoggerFactory.getLogger(ChatEventRelay.class);

    private final ChatWebSocketHandler sockets;
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final String mode;
    private final BooleanSupplier redisAvailability;
    private final RedisMessageListenerContainer listenerContainer;

    @Autowired
    public ChatEventRelay(
            ChatWebSocketHandler sockets,
            StringRedisTemplate redis,
            ObjectMapper json,
            @Value("${chat.redis.enabled:auto}") String mode,
            RedisAvailabilityChecker redisAvailability,
            Optional<RedisMessageListenerContainer> listenerContainer) {
        this(sockets, redis, json, mode, (BooleanSupplier) redisAvailability, listenerContainer.orElse(null));
    }

    public ChatEventRelay(
            ChatWebSocketHandler sockets,
            StringRedisTemplate redis,
            ObjectMapper json,
            String mode,
            BooleanSupplier redisAvailability) {
        this(sockets, redis, json, mode, redisAvailability, null);
    }

    private ChatEventRelay(
            ChatWebSocketHandler sockets,
            StringRedisTemplate redis,
            ObjectMapper json,
            String mode,
            BooleanSupplier redisAvailability,
            RedisMessageListenerContainer listenerContainer) {
        this.sockets = sockets;
        this.redis = redis;
        this.json = json;
        this.mode = mode == null ? "auto" : mode.trim().toLowerCase(Locale.ROOT);
        this.redisAvailability = redisAvailability;
        this.listenerContainer = listenerContainer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ChatEvent event) {
        if (!shouldUseRedis()) {
            sockets.deliver(event);
            return;
        }
        try {
            ensureListenerRunning();
            redis.convertAndSend(CHANNEL, json.writeValueAsString(event));
        } catch (RuntimeException ex) {
            // Persistence is already committed; a Redis outage must not turn a successful send into a failed REST response.
            log.warn("Chat relay unavailable; delivering locally. Other instances recover messages through history.");
            sockets.deliver(event);
        }
    }

    private boolean shouldUseRedis() {
        if ("false".equals(mode)) {
            return false;
        }
        if ("true".equals(mode)) {
            return true;
        }
        return redisAvailability != null && redisAvailability.getAsBoolean();
    }

    private void ensureListenerRunning() {
        if (listenerContainer != null && !listenerContainer.isRunning()) {
            synchronized (listenerContainer) {
                if (!listenerContainer.isRunning()) {
                    listenerContainer.start();
                }
            }
        }
    }
}
