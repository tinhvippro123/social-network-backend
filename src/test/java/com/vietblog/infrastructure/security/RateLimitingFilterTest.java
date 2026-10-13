package com.vietblog.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private final RateLimitingFilter filter = new RateLimitingFilter();

    @Test
    @DisplayName("Should allow requests within rate limit")
    void shouldAllowRequestsWithinLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/reports");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should block requests exceeding rate limit with HTTP 429")
    void shouldBlockRequestsExceedingLimit() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);
        String testIp = "192.168.1.200";

        // Limit for POST /api/reports is 20 requests per minute
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/reports");
            req.setRemoteAddr(testIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilterInternal(req, res, filterChain);
            assertEquals(200, res.getStatus());
        }

        // 21st request should be rate-limited (HTTP 429)
        MockHttpServletRequest excessReq = new MockHttpServletRequest("POST", "/api/reports");
        excessReq.setRemoteAddr(testIp);
        MockHttpServletResponse excessRes = new MockHttpServletResponse();

        filter.doFilterInternal(excessReq, excessRes, filterChain);

        assertEquals(429, excessRes.getStatus());
        assertTrue(excessRes.getContentAsString().contains("RATE_LIMIT_EXCEEDED"));
        assertEquals("60", excessRes.getHeader("Retry-After"));
    }

    @Test
    @DisplayName("OPTIONS preflight requests should not be rate-limited")
    void shouldBypassOptionsPreflight() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/reports");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}
