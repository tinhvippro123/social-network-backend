package com.vietblog.infrastructure.security;

import com.vietblog.application.service.SystemSettingsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MaintenanceModeFilterTest {

    private final SystemSettingsService settings = mock(SystemSettingsService.class);
    private final MaintenanceModeFilter filter = new MaintenanceModeFilter(settings);

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should pass through when maintenance mode is disabled")
    void allowsNormalTrafficWhenDisabled() throws ServletException, IOException {
        when(settings.isMaintenanceMode()).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should block regular user and guest API traffic with HTTP 503 during maintenance mode")
    void blocksRegularUserWhenEnabled() throws ServletException, IOException {
        when(settings.isMaintenanceMode()).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(503, response.getStatus());
        assertEquals("300", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("MAINTENANCE_MODE"));
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Should allow ADMIN, health probes, and auth login during maintenance mode")
    void allowsAdminHealthAndAuthDuringMaintenance() throws ServletException, IOException {
        when(settings.isMaintenanceMode()).thenReturn(true);
        FilterChain chain = mock(FilterChain.class);

        // 1. Health endpoint
        MockHttpServletRequest healthReq = new MockHttpServletRequest("GET", "/api/public/health");
        MockHttpServletResponse healthRes = new MockHttpServletResponse();
        filter.doFilterInternal(healthReq, healthRes, chain);
        assertEquals(200, healthRes.getStatus());

        // 2. Auth login endpoint (guest admin logging in)
        MockHttpServletRequest loginReq = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse loginRes = new MockHttpServletResponse();
        filter.doFilterInternal(loginReq, loginRes, chain);
        assertEquals(200, loginRes.getStatus());

        // 3. Authenticated ADMIN accessing normal routes
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin-1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
        MockHttpServletRequest adminPostsReq = new MockHttpServletRequest("GET", "/api/posts");
        MockHttpServletResponse adminPostsRes = new MockHttpServletResponse();
        filter.doFilterInternal(adminPostsReq, adminPostsRes, chain);
        assertEquals(200, adminPostsRes.getStatus());

        verify(chain, times(3)).doFilter(any(), any());
    }
}

