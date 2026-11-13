package com.vietblog.infrastructure.config;

import com.vietblog.infrastructure.messaging.ChatWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ChatWebSocketConfigTest {

    @Test
    void parseAllowedOriginsTrimsWhitespaceAndDropsEmptyEntries() {
        assertEquals(
                List.of("https://vietblog.vercel.app", "https://vietblog.domain.com"),
                SecurityConfig.parseAllowedOrigins(" https://vietblog.vercel.app , , https://vietblog.domain.com ")
        );
        assertEquals(List.of(), SecurityConfig.parseAllowedOrigins("   "));
        assertEquals(List.of(), SecurityConfig.parseAllowedOrigins(null));
    }

    @Test
    void registersWebSocketHandlersWithTrimmedAllowedOrigins() {
        ChatWebSocketHandler handler = mock(ChatWebSocketHandler.class);
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);
        when(registry.addHandler(handler, "/ws/chat", "/ws")).thenReturn(registration);

        ChatWebSocketConfig config = new ChatWebSocketConfig(
                handler,
                " https://vietblog.vercel.app , https://vietblog.domain.com , "
        );
        config.registerWebSocketHandlers(registry);

        verify(registration).setAllowedOrigins("https://vietblog.vercel.app", "https://vietblog.domain.com");
    }
}

