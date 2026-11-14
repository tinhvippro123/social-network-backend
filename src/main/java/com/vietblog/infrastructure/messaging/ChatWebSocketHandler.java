package com.vietblog.infrastructure.messaging;

import com.vietblog.application.dto.chat.ChatDtos.SendRequest;
import com.vietblog.application.service.ChatEvent;
import com.vietblog.application.service.ChatService;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.DomainException;
import com.vietblog.infrastructure.repository.UserRepository;
import com.vietblog.infrastructure.security.AuthCookieService;
import com.vietblog.infrastructure.security.JwtTokenProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Duration ACTIVE_CACHE_TTL = Duration.ofSeconds(15);
    private static final int MAX_ACTIVE_CACHE_ENTRIES = 5000;
    private static final int ACTIVE_QUERY_CHUNK_SIZE = 500;

    private record CachedActive(boolean active, long expiresAtMillis) {
        boolean isExpired(long nowMillis) {
            return nowMillis > expiresAtMillis;
        }
    }

    private final JwtTokenProvider tokens;
    private final UserRepository users;
    private final ChatService service;
    private final ObjectMapper json;
    private final Validator validator;
    private final AuthCookieService cookieService;
    private final ConcurrentMap<String, Client> clients = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CachedActive> activeCache = new ConcurrentHashMap<>();
    private final AutoCloseable invalidationRegistration;
    private final ScheduledExecutorService expiry = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "chat-session-expiry"); thread.setDaemon(true); return thread;
    });

    @Autowired
    public ChatWebSocketHandler(JwtTokenProvider tokens, UserRepository users, ChatService service,
            ObjectMapper json, Validator validator, AuthCookieService cookieService) {
        this.tokens = tokens; this.users = users; this.service = service;
        this.json = json; this.validator = validator; this.cookieService = cookieService;
        this.invalidationRegistration = User.CacheInvalidationListener.register(this::evictActiveUser);
        expiry.scheduleWithFixedDelay(this::expireSessions, 10, 10, TimeUnit.SECONDS);
    }

    @PostConstruct
    public void registerOnlineChecker() {
        if (this.service != null) {
            this.service.setOnlineChecker(this::isUserOnline);
        }
    }

    public boolean isUserOnline(String userId) {
        if (userId == null) return false;
        return clients.values().stream()
                .anyMatch(c -> userId.equals(c.userId()) && c.session() != null && c.session().isOpen());
    }

    public ChatWebSocketHandler(JwtTokenProvider tokens, UserRepository users, ChatService service,
            ObjectMapper json, Validator validator) {
        this(tokens, users, service, json, validator, AuthCookieService.defaultInstance());
    }

    public record Frame(String type, String token, String conversationId, SendRequest message) {}
    private record Client(WebSocketSession session, String userId, String token, long connectedAt) {}

    @Override public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(32768);
        long now = System.currentTimeMillis();
        String cookieToken = cookieService.extractAccessTokenFromHeaders(session.getHandshakeHeaders());
        if (cookieToken != null && validToken(cookieToken)) {
            String userId = tokens.getUserIdFromJWT(cookieToken);
            if (isUserActive(userId)) {
                clients.put(session.getId(), new Client(session, userId, cookieToken, now));
                try {
                    send(session, Map.of("type", "authenticated", "userId", userId));
                } catch (IOException ex) {
                    close(session, CloseStatus.SERVER_ERROR);
                }
                return;
            }
        }
        clients.put(session.getId(), new Client(session, null, null, now));
    }

    @Override protected void handleTextMessage(WebSocketSession session, TextMessage text) throws IOException {
        Client client = clients.get(session.getId());
        if (client == null) { close(session, CloseStatus.POLICY_VIOLATION); return; }
        try {
            Frame frame = json.readValue(text.getPayload(), Frame.class);
            if (client.userId() == null) {
                if (!"auth".equals(frame.type()) || frame.token() == null || !validAuthToken(frame.token())) {
                    close(session, CloseStatus.POLICY_VIOLATION); return;
                }
                String userId = tokens.getUserIdFromJWT(frame.token());
                if (!isUserActive(userId)) {
                    close(session, CloseStatus.POLICY_VIOLATION); return;
                }
                clients.put(session.getId(), new Client(session, userId, frame.token(), client.connectedAt()));
                send(session, Map.of("type", "authenticated", "userId", userId));
                return;
            }
            if (!validClient(client)) { close(session, CloseStatus.POLICY_VIOLATION); return; }
            if ("auth".equals(frame.type())) { send(session, Map.of("type", "authenticated", "userId", client.userId())); return; }
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
        List<Client> recipients = null;
        for (Client client : clients.values()) {
            if (!event.recipientId().equals(client.userId())) continue;
            if (!validAuthToken(client.token())) {
                close(client.session(), CloseStatus.POLICY_VIOLATION);
                continue;
            }
            if (recipients == null) recipients = new ArrayList<>(2);
            recipients.add(client);
        }
        if (recipients == null || recipients.isEmpty()) return;
        try {
            if (!isUserActive(event.recipientId())) {
                for (Client client : recipients) {
                    close(client.session(), CloseStatus.POLICY_VIOLATION);
                }
                return;
            }
            if (!service.canReceive(event.conversationId(), event.recipientId())) return;
            for (Client client : recipients) {
                try {
                    send(client.session(), event);
                } catch (IOException | RuntimeException ex) {
                    close(client.session(), CloseStatus.SERVER_ERROR);
                }
            }
        } catch (RuntimeException ex) {
            for (Client client : recipients) {
                close(client.session(), CloseStatus.SERVER_ERROR);
            }
        }
    }

    private boolean validToken(String token) {
        try { return tokens.validateToken(token); } catch (RuntimeException ex) { return false; }
    }
    private boolean validWsTicket(String token) {
        try { return tokens.validateWsTicket(token); } catch (RuntimeException ex) { return false; }
    }
    private boolean validAuthToken(String token) {
        return validToken(token) || validWsTicket(token);
    }
    private boolean validClient(Client client) {
        return validAuthToken(client.token()) && isUserActive(client.userId());
    }
    private boolean isUserActive(String userId) {
        if (userId == null) return false;
        long now = System.currentTimeMillis();
        CachedActive cached = activeCache.get(userId);
        if (cached != null && !cached.isExpired(now)) {
            return cached.active();
        }
        boolean active = users.findById(userId).map(u -> u.isActive()).orElse(false);
        cacheActiveUser(userId, active, now);
        return active;
    }
    private void refreshActiveStatuses(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        long now = System.currentTimeMillis();
        List<String> staleIds = new ArrayList<>();
        for (String userId : userIds) {
            if (userId == null) continue;
            CachedActive cached = activeCache.get(userId);
            if (cached == null || cached.isExpired(now)) {
                staleIds.add(userId);
            }
        }
        if (staleIds.isEmpty()) return;
        if (staleIds.size() == 1) {
            isUserActive(staleIds.get(0));
            return;
        }
        Set<String> activeIds = new HashSet<>();
        for (int i = 0; i < staleIds.size(); i += ACTIVE_QUERY_CHUNK_SIZE) {
            List<String> chunk = staleIds.subList(i, Math.min(staleIds.size(), i + ACTIVE_QUERY_CHUNK_SIZE));
            List<String> found = users.findActiveUserIds(chunk);
            if (found != null) {
                activeIds.addAll(found);
            }
        }
        for (String userId : staleIds) {
            cacheActiveUser(userId, activeIds.contains(userId), now);
        }
    }
    private void cacheActiveUser(String userId, boolean active, long nowMillis) {
        if (activeCache.size() >= MAX_ACTIVE_CACHE_ENTRIES) {
            activeCache.entrySet().removeIf(e -> e.getValue().isExpired(nowMillis));
            if (activeCache.size() >= MAX_ACTIVE_CACHE_ENTRIES) {
                activeCache.clear();
            }
        }
        activeCache.put(userId, new CachedActive(active, nowMillis + ACTIVE_CACHE_TTL.toMillis()));
    }
    private void evictActiveUser(String userId) {
        if (userId != null) {
            activeCache.remove(userId);
        }
    }
    private void send(WebSocketSession session, Object payload) throws IOException {
        synchronized (session) { if (session.isOpen()) session.sendMessage(new TextMessage(json.writeValueAsString(payload))); }
    }
    private void expireSessions() {
        long now = System.currentTimeMillis();
        List<Client> authed = null;
        Set<String> userIdsToCheck = null;
        for (Client client : clients.values()) {
            try {
                if (client.userId() == null) {
                    if (now - client.connectedAt() > 10000) {
                        close(client.session(), CloseStatus.POLICY_VIOLATION);
                    }
                    continue;
                }
                if (!validAuthToken(client.token())) {
                    close(client.session(), CloseStatus.POLICY_VIOLATION);
                    continue;
                }
                if (authed == null) {
                    authed = new ArrayList<>();
                    userIdsToCheck = new LinkedHashSet<>();
                }
                authed.add(client);
                userIdsToCheck.add(client.userId());
            } catch (RuntimeException ex) { close(client.session(), CloseStatus.SERVER_ERROR); }
        }
        if (authed == null || authed.isEmpty()) return;
        refreshActiveStatuses(userIdsToCheck);
        for (Client client : authed) {
            try {
                if (!isUserActive(client.userId())) {
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
        try { invalidationRegistration.close(); } catch (Exception ignored) { }
        expiry.shutdownNow();
        clients.values().forEach(client -> close(client.session(), CloseStatus.GOING_AWAY));
    }
}
