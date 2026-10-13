package com.vietblog.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filter to enforce rate limits on sensitive or high-volume API endpoints.
 * Protects against brute-force attacks, DDoS, and chat/report spamming.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    @Value("${app.rate-limiting.enabled:true}")
    private boolean enabled = true;

    private final Map<String, WindowCounter> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        int limit = resolveLimit(path, method);
        if (limit > 0) {
            String clientKey = resolveClientKey(request);
            String bucketKey = clientKey + ":" + path + ":" + method;

            if (!isAllowed(bucketKey, limit)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Retry-After", "60");
                response.getWriter().write("{\"success\":false,\"message\":\"Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.\",\"errorCode\":\"RATE_LIMIT_EXCEEDED\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private int resolveLimit(String path, String method) {
        // High-sensitivity write endpoints: Reports, Chat Messages, Auth
        if ("POST".equalsIgnoreCase(method)) {
            if (path.startsWith("/api/reports")) {
                return 20; // 20 reports per minute
            }
            if (path.contains("/messages")) {
                return 120; // 120 chat messages per minute (2 msgs/sec avg)
            }
            if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
                return 30; // 30 auth attempts per minute
            }
            if (path.startsWith("/api/posts")) {
                return 40; // 40 post operations per minute
            }
        }
        return 0; // Unmetered default for general GET requests
    }

    private String resolveClientKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private boolean isAllowed(String key, int maxRequestsPerMinute) {
        long currentMinute = System.currentTimeMillis() / 60000;
        cleanupStaleEntries(currentMinute);

        WindowCounter counter = requestCounts.compute(key, (k, existing) -> {
            if (existing == null || existing.minute != currentMinute) {
                return new WindowCounter(currentMinute, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        return counter.count.get() <= maxRequestsPerMinute;
    }

    private void cleanupStaleEntries(long currentMinute) {
        if (requestCounts.size() > 5000) {
            requestCounts.entrySet().removeIf(entry -> entry.getValue().minute < currentMinute - 2);
        }
    }

    private static class WindowCounter {
        final long minute;
        final AtomicInteger count;

        WindowCounter(long minute, AtomicInteger count) {
            this.minute = minute;
            this.count = count;
        }
    }
}
