package com.vietblog.infrastructure.messaging;

import com.vietblog.application.service.ChatEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

@Component
public class ChatEventRelay {
    public static final String CHANNEL = "vietblog.chat.events";
    private static final Logger log = LoggerFactory.getLogger(ChatEventRelay.class);
    private final ChatWebSocketHandler sockets;
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final boolean redisEnabled;
    public ChatEventRelay(ChatWebSocketHandler sockets, StringRedisTemplate redis, ObjectMapper json,
            @Value("${chat.redis.enabled:false}") boolean redisEnabled) {
        this.sockets = sockets; this.redis = redis; this.json = json; this.redisEnabled = redisEnabled;
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ChatEvent event) {
        if (!redisEnabled) { sockets.deliver(event); return; }
        try { redis.convertAndSend(CHANNEL, json.writeValueAsString(event)); }
        catch (RuntimeException ex) {
            // Persistence is already committed; a Redis outage must not turn a successful send into a failed REST response.
            log.warn("Chat relay unavailable; delivering locally. Other instances recover messages through history.");
            sockets.deliver(event);
        }
    }
}
