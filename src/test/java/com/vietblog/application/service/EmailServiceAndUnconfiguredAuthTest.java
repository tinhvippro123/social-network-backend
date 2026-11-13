package com.vietblog.application.service;

import com.vietblog.application.dto.auth.ForgotPasswordRequest;
import com.vietblog.application.dto.auth.RegisterRequest;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.infrastructure.repository.PasswordResetTokenRepository;
import com.vietblog.infrastructure.repository.UserRepository;
import com.vietblog.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceAndUnconfiguredAuthTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository resetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private SystemSettingsService systemSettingsService;

    @Test
    void isConfiguredReturnsFalseWhenHostBlankOrSenderMissing() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        EmailService noBean = new EmailService(emptyProvider, "", "", "VietBlog", "http://localhost:5173");
        assertThat(noBean.isConfigured()).isFalse();

        JavaMailSenderImpl blankHost = new JavaMailSenderImpl();
        blankHost.setHost("");
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> blankHostProvider = mock(ObjectProvider.class);
        when(blankHostProvider.getIfAvailable()).thenReturn(blankHost);
        EmailService blankHostService = new EmailService(blankHostProvider, "", "", "VietBlog", "http://localhost:5173");
        assertThat(blankHostService.isConfigured()).isFalse();

        JavaMailSenderImpl validHost = new JavaMailSenderImpl();
        validHost.setHost("smtp.gmail.com");
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> validProvider = mock(ObjectProvider.class);
        when(validProvider.getIfAvailable()).thenReturn(validHost);
        EmailService configuredService = new EmailService(validProvider, "sender@gmail.com", "", "VietBlog", "http://localhost:5173");
        assertThat(configuredService.isConfigured()).isTrue();
    }

    @Test
    void registerAndForgotPasswordRejectWith503CodeWhenSmtpUnconfigured() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        EmailService unconfiguredMail = new EmailService(emptyProvider, "", "", "VietBlog", "http://localhost:5173");

        AuthService authService = new AuthService(
                authenticationManager,
                userRepository,
                resetTokenRepository,
                passwordEncoder,
                tokenProvider,
                new ObjectMapper(),
                systemSettingsService,
                unconfiguredMail
        );

        when(systemSettingsService.isAllowRegistration()).thenReturn(true);
        when(systemSettingsService.isRequireEmailVerification()).thenReturn(true);
        when(userRepository.existsByEmail("new@vietblog.test")).thenReturn(false);

        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setName("Tester");
        registerReq.setEmail("new@vietblog.test");
        registerReq.setPassword("Password123!");
        registerReq.setPasswordConfirm("Password123!");

        assertThatThrownBy(() -> authService.register(registerReq))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_NOT_CONFIGURED);

        User activeUser = User.builder()
                .id("u1")
                .name("Active User")
                .email("active@vietblog.test")
                .isActive(true)
                .emailVerified(true)
                .build();
        when(userRepository.findByEmail("active@vietblog.test")).thenReturn(Optional.of(activeUser));

        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest();
        forgotReq.setEmail("active@vietblog.test");

        assertThatThrownBy(() -> authService.forgotPassword(forgotReq))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_NOT_CONFIGURED);
    }
}

