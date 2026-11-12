package com.vietblog.infrastructure.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

@Component
public class AuthCookieService {

    public static final String DEFAULT_ACCESS_COOKIE = "vietblog_access_token";
    public static final String DEFAULT_REFRESH_COOKIE = "vietblog_refresh_token";
    public static final String DEFAULT_STATUS_COOKIE = "vietblog_logged_in";

    private final String accessCookieName;
    private final String refreshCookieName;
    private final String statusCookieName;
    private final boolean secure;
    private final String sameSite;
    private final String domain;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    @Autowired
    public AuthCookieService(
            @Value("${jwt.cookie.access-name:vietblog_access_token}") String accessCookieName,
            @Value("${jwt.cookie.refresh-name:vietblog_refresh_token}") String refreshCookieName,
            @Value("${jwt.cookie.status-name:vietblog_logged_in}") String statusCookieName,
            @Value("${jwt.cookie.secure:false}") boolean secure,
            @Value("${jwt.cookie.same-site:Lax}") String sameSite,
            @Value("${jwt.cookie.domain:}") String domain,
            @Value("${jwt.access-token-expiration:86400000}") long accessExpirationMs,
            @Value("${jwt.refresh-token-expiration:604800000}") long refreshExpirationMs) {
        this.accessCookieName = accessCookieName;
        this.refreshCookieName = refreshCookieName;
        this.statusCookieName = statusCookieName;
        this.secure = secure;
        this.sameSite = sameSite;
        this.domain = domain;
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public static AuthCookieService defaultInstance() {
        return new AuthCookieService(
                DEFAULT_ACCESS_COOKIE,
                DEFAULT_REFRESH_COOKIE,
                DEFAULT_STATUS_COOKIE,
                false,
                "Lax",
                "",
                86_400_000L,
                604_800_000L
        );
    }

    public void writeAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(accessCookieName, accessToken, "/", true, Duration.ofMillis(accessExpirationMs)).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(refreshCookieName, refreshToken, "/api/auth", true, Duration.ofMillis(refreshExpirationMs)).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(statusCookieName, "1", "/", false, Duration.ofMillis(refreshExpirationMs)).toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(accessCookieName, "", "/", true, Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(refreshCookieName, "", "/api/auth", true, Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(statusCookieName, "", "/", false, Duration.ZERO).toString());
    }

    public String extractAccessToken(HttpServletRequest request) {
        return extractServletCookie(request, accessCookieName);
    }

    public String extractRefreshToken(HttpServletRequest request) {
        return extractServletCookie(request, refreshCookieName);
    }

    public String extractAccessTokenFromHeaders(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        List<String> cookieHeaders = headers.get(HttpHeaders.COOKIE);
        if (cookieHeaders == null || cookieHeaders.isEmpty()) {
            return null;
        }
        String prefix = accessCookieName + "=";
        for (String header : cookieHeaders) {
            if (!StringUtils.hasText(header)) {
                continue;
            }
            for (String rawPair : header.split(";")) {
                String pair = rawPair.trim();
                if (pair.startsWith(prefix)) {
                    String value = pair.substring(prefix.length()).trim();
                    if (StringUtils.hasText(value)) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    private String extractServletCookie(HttpServletRequest request, String cookieName) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue().trim();
            }
        }
        return null;
    }

    private ResponseCookie buildCookie(String name, String value, String path, boolean httpOnly, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(httpOnly)
                .secure(secure)
                .path(path)
                .maxAge(maxAge)
                .sameSite(sameSite);
        if (StringUtils.hasText(domain)) {
            builder.domain(domain.trim());
        }
        return builder.build();
    }
}

