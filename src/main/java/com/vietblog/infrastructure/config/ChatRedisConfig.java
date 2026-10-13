package com.vietblog.infrastructure.config;

import com.vietblog.application.service.ChatEvent;
import com.vietblog.infrastructure.messaging.ChatEventRelay;
import com.vietblog.infrastructure.messaging.ChatWebSocketHandler;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.*;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;

@Configuration @ConditionalOnProperty(name = "chat.redis.enabled", havingValue = "true")
public class ChatRedisConfig {
    @Bean
    public RedisMessageListenerContainer chatRedisListener(RedisConnectionFactory connectionFactory, ObjectMapper json, ChatWebSocketHandler sockets) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            try { sockets.deliver(json.readValue(new String(message.getBody(), StandardCharsets.UTF_8), ChatEvent.class)); }
            catch (RuntimeException ex) { LoggerFactory.getLogger(ChatRedisConfig.class).warn("Discarded invalid chat relay event"); }
        }, new ChannelTopic(ChatEventRelay.CHANNEL));
        return container;
    }
}
