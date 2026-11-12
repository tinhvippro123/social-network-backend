package com.vietblog;

import com.vietblog.domain.entity.User;
import com.vietblog.infrastructure.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class CookieAuthenticationIntegrationTest extends VietblogBackendApplicationTests {

    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    private MockMvc mvc;
    private String email;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        email = "cookie-" + UUID.randomUUID() + "@vietblog.test";
        users.saveAndFlush(User.builder()
                .name("Cookie Tester")
                .email(email)
                .password(passwordEncoder.encode("Password123!"))
                .role(User.Role.USER)
                .isActive(true)
                .build());
    }

    @Test
    void loginSetsHttpOnlyCookiesOmitsTokenInJsonAndSupportsRefreshAndLogout() throws Exception {
        MvcResult loginResult = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie("vietblog_access_token");
        Cookie refreshCookie = loginResult.getResponse().getCookie("vietblog_refresh_token");
        Cookie statusCookie = loginResult.getResponse().getCookie("vietblog_logged_in");

        assertThat(accessCookie).isNotNull();
        assertThat(accessCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(statusCookie).isNotNull();
        assertThat(statusCookie.isHttpOnly()).isFalse();
        assertThat(statusCookie.getValue()).isEqualTo("1");

        // Authenticate /api/auth/me purely via HttpOnly cookie (no Authorization header)
        mvc.perform(get("/api/auth/me").cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));

        // Refresh token cannot be abused as an access token
        Cookie forgedAccess = new Cookie("vietblog_access_token", refreshCookie.getValue());
        mvc.perform(get("/api/auth/me").cookie(forgedAccess))
                .andExpect(status().isUnauthorized());

        // Access token cannot be abused as a refresh token, and missing cookie returns 401
        Cookie forgedRefresh = new Cookie("vietblog_refresh_token", accessCookie.getValue());
        mvc.perform(post("/api/auth/refresh").cookie(forgedRefresh))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());

        // Silent refresh works with empty body using only the HttpOnly refresh cookie
        MvcResult refreshResult = mvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andReturn();

        Cookie rotatedAccess = refreshResult.getResponse().getCookie("vietblog_access_token");
        assertThat(rotatedAccess).isNotNull();
        mvc.perform(get("/api/auth/me").cookie(rotatedAccess))
                .andExpect(status().isOk());

        // Logout expires all cookies
        MvcResult logoutResult = mvc.perform(post("/api/auth/logout").cookie(rotatedAccess))
                .andExpect(status().isOk())
                .andReturn();
        List<String> cleared = logoutResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cleared).hasSize(3).allSatisfy(h -> assertThat(h).contains("Max-Age=0"));
    }

    @Test
    void wsTicketRequiresAuthCannotAccessRestAndPublicHealthIsOpen() throws Exception {
        mvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));

        mvc.perform(post("/api/auth/ws-ticket"))
                .andExpect(status().isUnauthorized());

        MvcResult loginResult = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie("vietblog_access_token");
        assertThat(accessCookie).isNotNull();

        MvcResult ticketResult = mvc.perform(post("/api/auth/ws-ticket").cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ticket").isString())
                .andReturn();

        String body = ticketResult.getResponse().getContentAsString();
        String ticket = body.replaceAll(".*\"ticket\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        assertThat(ticket).isNotBlank().isNotEqualTo(body);

        // WS ticket must NOT authenticate REST endpoints
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + ticket))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").cookie(new Cookie("vietblog_access_token", ticket)))
                .andExpect(status().isUnauthorized());
    }
}

