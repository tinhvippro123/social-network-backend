package com.vietblog;

import com.vietblog.application.service.*;
import com.vietblog.domain.entity.User;
import com.vietblog.infrastructure.messaging.ChatWebSocketHandler;
import com.vietblog.infrastructure.repository.UserRepository;
import com.vietblog.infrastructure.security.JwtTokenProvider;
import jakarta.validation.Validator;
import org.junit.jupiter.api.*;
import org.springframework.web.socket.*;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatWebSocketTests {
    private JwtTokenProvider tokens;
    private ChatService chat;
    private ChatWebSocketHandler handler;
    private WebSocketSession session;
    @BeforeEach void setup() {
        tokens = mock(JwtTokenProvider.class);
        chat = mock(ChatService.class);
        UserRepository users = mock(UserRepository.class);
        when(users.findById("user-1")).thenReturn(Optional.of(User.builder().id("user-1").isActive(true).build()));
        handler = new ChatWebSocketHandler(tokens, users, chat, JsonMapper.builder().build(), mock(Validator.class));
        session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);
    }
    @AfterEach void cleanup() { handler.shutdown(); }

    @Test void unauthenticatedClientsCannotSendMessages() throws Exception {
        handler.handleMessage(session, new TextMessage("{\"type\":\"message.send\",\"conversationId\":\"secret\"}"));
        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verifyNoInteractions(chat);
    }

    @Test void recipientAndCurrentMembershipAreBothRequiredForDelivery() throws Exception {
        authenticate();
        clearInvocations(session);
        handler.deliver(new ChatEvent("message.created", "conversation", "another-user", Map.of("content", "private")));
        verify(session, never()).sendMessage(any());
        handler.deliver(new ChatEvent("message.created", "conversation", "user-1", Map.of("content", "private")));
        verify(session, never()).sendMessage(any());
        when(chat.canReceive("conversation", "user-1")).thenReturn(true);
        handler.deliver(new ChatEvent("message.created", "conversation", "user-1", Map.of("content", "private")));
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test void expiredTokensStopDelivery() throws Exception {
        authenticate();
        clearInvocations(session);
        when(tokens.validateToken("valid")).thenReturn(false);
        handler.deliver(new ChatEvent("message.created", "conversation", "user-1", Map.of("content", "private")));
        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(session, never()).sendMessage(any());
    }

    private void authenticate() throws Exception {
        when(tokens.validateToken("valid")).thenReturn(true);
        when(tokens.getUserIdFromJWT("valid")).thenReturn("user-1");
        handler.handleMessage(session, new TextMessage("{\"type\":\"auth\",\"token\":\"valid\"}"));
    }
}
