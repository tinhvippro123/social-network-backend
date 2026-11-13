package com.vietblog.infrastructure.security;

import com.vietblog.application.service.SystemSettingsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet security filter enforcing the Admin {@code maintenanceMode} setting at runtime.
 * Registered inside {@link com.vietblog.infrastructure.config.SecurityConfig} after JWT & rate limiting
 * so authenticated {@code ROLE_ADMIN} sessions retain full access while public/regular traffic receives HTTP 503.
 */
@RequiredArgsConstructor
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private final SystemSettingsService systemSettingsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (path == null || (!path.startsWith("/api/") && !path.startsWith("/ws"))) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isExemptPath(path) || isAdminAuthenticated() || !systemSettingsService.isMaintenanceMode()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", "300");
        response.getWriter().write(
                "{\"success\":false,\"message\":\"Hệ thống đang bảo trì. Vui lòng quay lại sau.\",\"errorCode\":\"MAINTENANCE_MODE\"}"
        );
    }

    private boolean isExemptPath(String path) {
        return path.startsWith("/api/public/")
                || path.startsWith("/api/admin/")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/google")
                || path.equals("/api/auth/refresh")
                || path.equals("/api/auth/logout")
                || path.equals("/api/auth/me");
    }

    private boolean isAdminAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}

