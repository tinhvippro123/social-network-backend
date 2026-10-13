package com.vietblog.application.service;

import com.vietblog.infrastructure.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AdminAuthorizationTest {
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() { context = new AnnotationConfigApplicationContext(Config.class); }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void memberCannotAccessAdminDataOrChangeRoles() {
        authenticate("ROLE_USER");
        AdminService admin = context.getBean(AdminService.class);
        assertThrows(AccessDeniedException.class, admin::stats);
        assertThrows(AccessDeniedException.class,
                () -> admin.changeRole("target", "member", com.vietblog.domain.entity.User.Role.ADMIN));
        verifyNoInteractions(context.getBean(AdminUserRepository.class));
    }

    @Test
    void moderatorCannotReadPrivateAdminUserDirectory() {
        authenticate("ROLE_MODERATOR");
        assertThrows(AccessDeniedException.class,
                () -> context.getBean(AdminService.class).users(null, "", 1, 20));
        verifyNoInteractions(context.getBean(AdminUserRepository.class));
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("actor", "unused", AuthorityUtils.createAuthorityList(role)));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean AdminUserRepository users() { return mock(AdminUserRepository.class); }
        @Bean AdminPostRepository posts() { return mock(AdminPostRepository.class); }
        @Bean CommentRepository comments() { return mock(CommentRepository.class); }
        @Bean ReportRepository reports() { return mock(ReportRepository.class); }
        @Bean GroupRepository groups() { return mock(GroupRepository.class); }
        @Bean AdminService admin(AdminUserRepository users, AdminPostRepository posts, CommentRepository comments,
                                 ReportRepository reports, GroupRepository groups) {
            return new AdminService(users, posts, comments, reports, groups);
        }
    }
}
