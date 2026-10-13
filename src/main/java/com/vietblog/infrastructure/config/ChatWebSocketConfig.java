package com.vietblog.infrastructure.config;

import com.vietblog.infrastructure.messaging.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration @EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {
    private final ChatWebSocketHandler handler;
    private final String[] allowedOrigins;
    public ChatWebSocketConfig(ChatWebSocketHandler handler,
            @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://localhost:3000}") String[] allowedOrigins) {
        this.handler = handler; this.allowedOrigins = allowedOrigins;
    }
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/chat").setAllowedOrigins(allowedOrigins);
    }
}
