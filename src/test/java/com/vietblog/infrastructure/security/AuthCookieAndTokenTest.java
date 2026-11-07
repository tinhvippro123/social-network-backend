package com.vietblog.infrastructure.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieAndTokenTest {

    private JwtTokenProvider tokenProvider;
    private AuthCookieService cookieService;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret",
                "VietBlog2026SecretKeyForJWTTokenGenerationMustBeAtLeast256Bits");
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationInMs", 86_400_000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshExpirationInMs", 604_800_000L);

        cookieService = new AuthCookieService(
                "vietblog_access_token",
                "vietblog_refresh_token",
                "vietblog_logged_in",
                false,
                "Lax",
                "",
                86_400_000L,
                604_800_000L
        );
    }

    @Test
    void accessAndRefreshTokensAreStrictlyIsolatedByType() {
        String access = tokenProvider.generateTokenFromUserId("user-123");
        String refresh = tokenProvider.generateRefreshTokenFromUserId("user-123");

        assertThat(tokenProvider.validateToken(access)).isTrue();
        assertThat(tokenProvider.validateRefreshToken(access)).isFalse();

        assertThat(tokenProvider.validateRefreshToken(refresh)).isTrue();
        assertThat(tokenProvider.validateToken(refresh)).isFalse();

        assertThat(tokenProvider.getUserIdFromJWT(access)).isEqualTo("user-123");
        assertThat(tokenProvider.getUserIdFromJWT(refresh)).isEqualTo("user-123");
    }

    @Test
    void writeAndClearCookiesSetExpectedSecurityAttributes() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        cookieService.writeAuthCookies(response, "acc-jwt", "ref-jwt");

        List<String> setCookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).hasSize(3);
        assertThat(setCookies).anySatisfy(c -> {
            assertThat(c).contains("vietblog_access_token=acc-jwt");
            assertThat(c).contains("HttpOnly");
            assertThat(c).contains("Path=/");
            assertThat(c).contains("SameSite=Lax");
        });
        assertThat(setCookies).anySatisfy(c -> {
            assertThat(c).contains("vietblog_refresh_token=ref-jwt");
            assertThat(c).contains("HttpOnly");
            assertThat(c).contains("Path=/api/auth");
            assertThat(c).contains("SameSite=Lax");
        });
        assertThat(setCookies).anySatisfy(c -> {
            assertThat(c).contains("vietblog_logged_in=1");
            assertThat(c).doesNotContain("HttpOnly");
            assertThat(c).contains("Path=/");
        });

        MockHttpServletResponse clearResponse = new MockHttpServletResponse();
        cookieService.clearAuthCookies(clearResponse);
        assertThat(clearResponse.getHeaders(HttpHeaders.SET_COOKIE))
                .hasSize(3)
                .allSatisfy(c -> assertThat(c).contains("Max-Age=0"));
    }

    @Test
    void extractsTokensFromServletCookiesAndWebSocketHandshakeHeaders() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(
                new Cookie("vietblog_access_token", "cookie-access"),
                new Cookie("vietblog_refresh_token", "cookie-refresh")
        );
        assertThat(cookieService.extractAccessToken(req)).isEqualTo("cookie-access");
        assertThat(cookieService.extractRefreshToken(req)).isEqualTo("cookie-refresh");

        HttpHeaders wsHeaders = new HttpHeaders();
        wsHeaders.add(HttpHeaders.COOKIE, "vietblog_theme=dark; vietblog_access_token=ws-jwt; vietblog_logged_in=1");
        assertThat(cookieService.extractAccessTokenFromHeaders(wsHeaders)).isEqualTo("ws-jwt");
    }
}

