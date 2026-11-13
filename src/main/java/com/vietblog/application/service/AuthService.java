package com.vietblog.application.service;

import com.vietblog.application.dto.auth.*;
import com.vietblog.domain.entity.PasswordResetToken;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.exception.ForbiddenException;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.infrastructure.repository.PasswordResetTokenRepository;
import com.vietblog.infrastructure.repository.UserRepository;
import com.vietblog.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final SystemSettingsService systemSettingsService;
    private final EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_EXPIRY_MINUTES = 15;

    private record IssuedOtpToken(String token, String otp, LocalDateTime expiryDate) {}

    @org.springframework.beans.factory.annotation.Value("${google.client-id:}")
    private String googleClientId;

    @Transactional
    public AuthSessionResult login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (isEmailVerificationRequired() && !user.isEmailVerifiedEffective()) {
            throw new BusinessRuleException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = tokenProvider.generateToken(authentication);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String refreshToken = tokenProvider.generateRefreshTokenFromUserId(user.getId());
        return new AuthSessionResult(accessToken, refreshToken, JwtAuthenticationResponse.UserDto.fromEntity(user));
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (systemSettingsService != null && !systemSettingsService.isAllowRegistration()) {
            throw new BusinessRuleException(ErrorCode.REGISTRATION_DISABLED);
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (request.getPasswordConfirm() != null && !request.getPasswordConfirm().isBlank()
                && !request.getPassword().equals(request.getPasswordConfirm())) {
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT, "Mật khẩu xác nhận không khớp");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessRuleException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        boolean requireVerification = isEmailVerificationRequired();
        if (requireVerification && !emailService.isConfigured()) {
            throw new BusinessRuleException(ErrorCode.EMAIL_NOT_CONFIGURED);
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .emailVerified(!requireVerification)
                .build();

        userRepository.save(user);

        if (requireVerification) {
            IssuedOtpToken issued = issueOtpToken(user, PasswordResetToken.TokenPurpose.EMAIL_VERIFICATION);
            emailService.sendVerificationOtp(user, issued.otp(), OTP_EXPIRY_MINUTES);
            return RegisterResponse.builder()
                    .requiresVerification(true)
                    .email(normalizedEmail)
                    .message("Đăng ký thành công. Vui lòng kiểm tra email để nhập mã OTP xác thực tài khoản.")
                    .build();
        }

        return RegisterResponse.builder()
                .requiresVerification(false)
                .email(normalizedEmail)
                .message("Đăng ký thành công. Bạn có thể đăng nhập ngay.")
                .build();
    }

    @Transactional
    public AuthSessionResult verifyEmail(VerifyEmailRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        PasswordResetToken verificationToken = resolveOtpOrToken(
                request.getOtp().trim(),
                normalizedEmail,
                PasswordResetToken.TokenPurpose.EMAIL_VERIFICATION,
                ErrorCode.INVALID_VERIFICATION_TOKEN,
                ErrorCode.VERIFICATION_TOKEN_EXPIRED
        );

        User user = verificationToken.getUser();
        if (!user.isActive()) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        user.setEmailVerified(true);
        verificationToken.setUsed(true);
        resetTokenRepository.save(verificationToken);

        return issueSession(user);
    }

    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .or(() -> userRepository.findByEmail(request.getEmail().trim()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        if (!user.isActive()) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }
        if (user.isEmailVerifiedEffective()) {
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT, "Email này đã được xác thực. Vui lòng đăng nhập.");
        }

        IssuedOtpToken issued = issueOtpToken(user, PasswordResetToken.TokenPurpose.EMAIL_VERIFICATION);
        emailService.sendVerificationOtp(user, issued.otp(), OTP_EXPIRY_MINUTES);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .or(() -> userRepository.findByEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessRuleException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .or(() -> userRepository.findByEmail(request.getEmail().trim()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        IssuedOtpToken issued = issueOtpToken(user, PasswordResetToken.TokenPurpose.PASSWORD_RESET);
        emailService.sendPasswordResetOtp(user, issued.otp(), OTP_EXPIRY_MINUTES);

        log.info("[PASSWORD_RESET] Sent reset OTP email for user id={}, expiresAt={}", user.getId(), issued.expiryDate());

        return ForgotPasswordResponse.builder()
                .message("Mã OTP đặt lại mật khẩu đã được gửi về email của bạn")
                .expiresInSeconds(OTP_EXPIRY_MINUTES * 60L)
                .build();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenOrOtp = request.getToken().trim();
        String emailFilter = (request.getEmail() != null && !request.getEmail().isBlank())
                ? request.getEmail().trim()
                : null;

        PasswordResetToken resetToken = resolveOtpOrToken(
                tokenOrOtp,
                emailFilter,
                PasswordResetToken.TokenPurpose.PASSWORD_RESET,
                ErrorCode.INVALID_RESET_TOKEN,
                ErrorCode.RESET_TOKEN_EXPIRED
        );

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        log.info("[PASSWORD_RESET] Password successfully reset for user id={}", user.getId());
    }

    private boolean isEmailVerificationRequired() {
        return systemSettingsService == null || systemSettingsService.isRequireEmailVerification();
    }

    private IssuedOtpToken issueOtpToken(User user, PasswordResetToken.TokenPurpose purpose) {
        for (PasswordResetToken existing : resetTokenRepository.findByUserAndUsedFalse(user)) {
            if (existing.effectivePurpose() == purpose) {
                existing.setUsed(true);
                resetTokenRepository.save(existing);
            }
        }

        String resetToken = UUID.randomUUID().toString();
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        PasswordResetToken token = PasswordResetToken.builder()
                .token(resetToken)
                .otp(otp)
                .user(user)
                .expiryDate(expiryDate)
                .used(false)
                .purpose(purpose)
                .build();

        resetTokenRepository.save(token);
        return new IssuedOtpToken(resetToken, otp, expiryDate);
    }

    private PasswordResetToken resolveOtpOrToken(
            String tokenOrOtp,
            String emailFilter,
            PasswordResetToken.TokenPurpose expectedPurpose,
            ErrorCode invalidCode,
            ErrorCode expiredCode) {
        Optional<PasswordResetToken> tokenOpt = resetTokenRepository.findByTokenAndUsedFalse(tokenOrOtp)
                .filter(t -> t.effectivePurpose() == expectedPurpose)
                .filter(t -> emailFilter == null || (t.getUser() != null && emailFilter.equalsIgnoreCase(t.getUser().getEmail())));

        if (tokenOpt.isEmpty()) {
            if (emailFilter != null) {
                tokenOpt = resetTokenRepository
                        .findAllByOtpAndUserEmailIgnoreCaseAndUsedFalseOrderByExpiryDateDesc(tokenOrOtp, emailFilter)
                        .stream()
                        .filter(t -> t.effectivePurpose() == expectedPurpose)
                        .findFirst();
            } else {
                tokenOpt = resetTokenRepository.findFirstByOtpAndUsedFalseOrderByExpiryDateDesc(tokenOrOtp)
                        .filter(t -> t.effectivePurpose() == expectedPurpose);
            }
        }

        PasswordResetToken matched = tokenOpt.orElseThrow(() -> new BusinessRuleException(invalidCode));
        if (matched.isExpired()) {
            throw new BusinessRuleException(expiredCode);
        }
        return matched;
    }

    private AuthSessionResult issueSession(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        String accessToken = tokenProvider.generateTokenFromUserId(user.getId());
        String refreshToken = tokenProvider.generateRefreshTokenFromUserId(user.getId());
        return new AuthSessionResult(accessToken, refreshToken, JwtAuthenticationResponse.UserDto.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public AuthSessionResult refreshToken(String token) {
        if (token == null || token.isBlank() || !tokenProvider.validateRefreshToken(token)) {
            throw new BusinessRuleException(ErrorCode.UNAUTHORIZED);
        }

        String userId = tokenProvider.getUserIdFromJWT(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        String newAccess = tokenProvider.generateTokenFromUserId(userId);
        String newRefresh = tokenProvider.generateRefreshTokenFromUserId(userId);
        return new AuthSessionResult(newAccess, newRefresh, JwtAuthenticationResponse.UserDto.fromEntity(user));
    }

    @Transactional
    public AuthSessionResult loginWithGoogle(GoogleLoginRequest request) {
        String email = null;
        String name = null;
        String picture = null;

        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            if (request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                        .header("Authorization", "Bearer " + request.getAccessToken().trim())
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() == 200) {
                    Map<?, ?> body = objectMapper.readValue(httpResponse.body(), Map.class);
                    Object audObj = body.get("aud");
                    if (googleClientId != null && !googleClientId.isBlank()
                            && audObj != null && !googleClientId.equals(String.valueOf(audObj))) {
                        throw new BusinessRuleException(ErrorCode.UNAUTHORIZED);
                    }
                    if (Boolean.FALSE.equals(body.get("email_verified"))) {
                        throw new BusinessRuleException(ErrorCode.UNAUTHORIZED);
                    }
                    email = (String) body.get("email");
                    name = (String) body.get("name");
                    picture = (String) body.get("picture");
                } else {
                    log.error("Google userinfo failed with status {}: {}", httpResponse.statusCode(), httpResponse.body());
                }
            } else if (request.getIdToken() != null && !request.getIdToken().isBlank()) {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + request.getIdToken().trim()))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() == 200) {
                    Map<?, ?> body = objectMapper.readValue(httpResponse.body(), Map.class);
                    if (googleClientId != null && !googleClientId.isBlank()
                            && !googleClientId.equals(String.valueOf(body.get("aud")))) {
                        throw new BusinessRuleException(ErrorCode.UNAUTHORIZED);
                    }
                    if (Boolean.FALSE.equals(body.get("email_verified"))
                            || "false".equalsIgnoreCase(String.valueOf(body.get("email_verified")))) {
                        throw new BusinessRuleException(ErrorCode.UNAUTHORIZED);
                    }
                    email = (String) body.get("email");
                    name = (String) body.get("name");
                    picture = (String) body.get("picture");
                } else {
                    log.error("Google tokeninfo failed with status {}: {}", httpResponse.statusCode(), httpResponse.body());
                }
            }
        } catch (Exception e) {
            log.error("Error communicating with Google API", e);
            throw new BusinessRuleException(ErrorCode.UNAUTHORIZED);
        }

        if (email == null || email.isBlank()) {
            throw new BusinessRuleException(ErrorCode.UNAUTHORIZED);
        }

        final String finalEmail = email.toLowerCase().trim();
        final String finalName = (name != null && !name.isBlank()) ? name.trim() : finalEmail.split("@")[0];
        final String finalPicture = picture;

        User user = userRepository.findByEmail(finalEmail).orElseGet(() -> {
            if (systemSettingsService != null) {
                Object allowReg = systemSettingsService.getAll().get("allowRegistration");
                if (Boolean.FALSE.equals(allowReg)) {
                    throw new BusinessRuleException(ErrorCode.INVALID_INPUT, "Hệ thống tạm thời đóng đăng ký tài khoản mới");
                }
            }
            User newUser = User.builder()
                    .email(finalEmail)
                    .name(finalName)
                    .avatar(finalPicture)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(User.Role.USER)
                    .isActive(true)
                    .emailVerified(true)
                    .joinedAt(LocalDateTime.now())
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            return userRepository.save(newUser);
        });

        if (!user.isActive()) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        if ((user.getAvatar() == null || user.getAvatar().isBlank()) && finalPicture != null && !finalPicture.isBlank()) {
            user.setAvatar(finalPicture);
        }
        user.setEmailVerified(true);
        return issueSession(user);
    }

    public String createWebSocketTicket(String userId) {
        return tokenProvider.generateWsTicketFromUserId(userId);
    }
}
