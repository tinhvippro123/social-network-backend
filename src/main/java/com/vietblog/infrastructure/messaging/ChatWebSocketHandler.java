package com.vietblog.infrastructure.messaging;

import com.vietblog.application.dto.chat.ChatDtos.SendRequest;
import com.vietblog.application.service.ChatEvent;
import com.vietblog.application.service.ChatService;
import com.vietblog.domain.exception.DomainException;
import com.vietblog.infrastructure.repository.UserRepository;
import com.vietblog.infrastructure.security.JwtTokenProvider;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final JwtTokenProvider tokens;
    private final UserRepository users;
    private final ChatService service;
    private final ObjectMapper json;
    private final Validator validator;
    private final ConcurrentMap<String, Client> clients = new ConcurrentHashMap<>();
    private final ScheduledExecutorService expiry = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "chat-session-expiry"); thread.setDaemon(true); return thread;
    });

    public ChatWebSocketHandler(JwtTokenProvider tokens, UserRepository users, ChatService service, ObjectMapper json, Validator validator) {
        this.tokens = tokens; this.users = users; this.service = service; this.json = json; this.validator = validator;
        expiry.scheduleWithFixedDelay(this::expireSessions, 10, 10, TimeUnit.SECONDS);
    }

    public record Frame(String type, String token, String conversationId, SendRequest message) {}
    private record Client(WebSocketSession session, String userId, String token, long connectedAt) {}

    @Override public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(32768);
        clients.put(session.getId(), new Client(session, null, null, System.currentTimeMillis()));
    }

    @Override protected void handleTextMessage(WebSocketSession session, TextMessage text) throws IOException {
        Client client = clients.get(session.getId());
        if (client == null) { close(session, CloseStatus.POLICY_VIOLATION); return; }
        try {
            Frame frame = json.readValue(text.getPayload(), Frame.class);
            if (client.userId() == null) {
                if (!"auth".equals(frame.type()) || frame.token() == null || !validToken(frame.token())) {
                    close(session, CloseStatus.POLICY_VIOLATION); return;
                }
                String userId = tokens.getUserIdFromJWT(frame.token());
                if (!users.findById(userId).map(u -> u.isActive()).orElse(false)) {
                    close(session, CloseStatus.POLICY_VIOLATION); return;
                }
                clients.put(session.getId(), new Client(session, userId, frame.token(), client.connectedAt()));
                send(session, Map.of("type", "authenticated", "userId", userId));
                return;
            }
            if (!validClient(client)) { close(session, CloseStatus.POLICY_VIOLATION); return; }
            if ("ping".equals(frame.type())) { send(session, Map.of("type", "pong")); return; }
            if (frame.conversationId() == null || frame.conversationId().isBlank()) throw new IllegalArgumentException("conversationId is required");
            switch (frame.type() == null ? "" : frame.type()) {
                case "message.send" -> {
                    if (frame.message() == null || !validator.validate(frame.message()).isEmpty()) throw new IllegalArgumentException("Invalid message");
                    var sent = service.send(frame.conversationId(), client.userId(), frame.message());
                    send(session, Map.of("type", "message.accepted", "messageId", sent.id()));
                }
                case "conversation.read" -> service.read(frame.conversationId(), client.userId());
                default -> throw new IllegalArgumentException("Unsupported frame type");
            }
        } catch (DomainException ex) {
            send(session, Map.of("type", "error", "errorCode", ex.getErrorCode().name()));
        } catch (RuntimeException ex) {
            send(session, Map.of("type", "error", "errorCode", "INVALID_INPUT"));
        }
    }

    /** Only server-addressed recipients receive data; clients cannot subscribe to arbitrary channels. */
    public void deliver(ChatEvent event) {
        for (Client client : clients.values()) {
            if (!event.recipientId().equals(client.userId())) continue;
            try {
                if (!validClient(client)) { close(client.session(), CloseStatus.POLICY_VIOLATION); continue; }
                if (service.canReceive(event.conversationId(), client.userId())) send(client.session(), event);
            } catch (IOException | RuntimeException ex) { close(client.session(), CloseStatus.SERVER_ERROR); }
        }
    }

    private boolean validToken(String token) {
        try { return tokens.validateToken(token); } catch (RuntimeException ex) { return false; }
    }
    private boolean validClient(Client client) {
        return validToken(client.token()) && users.findById(client.userId()).map(u -> u.isActive()).orElse(false);
    }
    private void send(WebSocketSession session, Object payload) throws IOException {
        synchronized (session) { if (session.isOpen()) session.sendMessage(new TextMessage(json.writeValueAsString(payload))); }
    }
    private void expireSessions() {
        for (Client client : clients.values()) {
            try {
                if (client.userId() == null ? System.currentTimeMillis() - client.connectedAt() > 10000 : !validClient(client)) {
                    close(client.session(), CloseStatus.POLICY_VIOLATION);
                }
            } catch (RuntimeException ex) { close(client.session(), CloseStatus.SERVER_ERROR); }
        }
    }
    private void close(WebSocketSession session, CloseStatus status) {
        clients.remove(session.getId());
        try { session.close(status); } catch (IOException ignored) { }
    }
    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { clients.remove(session.getId()); }
    @Override public void handleTransportError(WebSocketSession session, Throwable error) { close(session, CloseStatus.SERVER_ERROR); }
    @PreDestroy public void shutdown() {
        expiry.shutdownNow();
        clients.values().forEach(client -> close(client.session(), CloseStatus.GOING_AWAY));
    }
}
