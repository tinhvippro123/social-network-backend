package com.vietblog.application.service;

import com.vietblog.application.dto.auth.*;
import com.vietblog.domain.entity.PasswordResetToken;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
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
import java.time.Duration;
import java.security.SecureRandom;
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

    public JwtAuthenticationResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return new JwtAuthenticationResponse(jwt, JwtAuthenticationResponse.UserDto.fromEntity(user));
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
    }

    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessRuleException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        String resetToken = UUID.randomUUID().toString();
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(15);

        PasswordResetToken token = PasswordResetToken.builder()
                .token(resetToken)
                .otp(otp)
                .user(user)
                .expiryDate(expiryDate)
                .used(false)
                .build();

        resetTokenRepository.save(token);

        log.info("[PASSWORD_RESET] Created reset token for email={}: token={}, otp={}, expiresAt={}",
                user.getEmail(), resetToken, otp, expiryDate);

        return ForgotPasswordResponse.builder()
                .message("Mã xác nhận đặt lại mật khẩu đã được tạo thành công")
                .resetToken(resetToken)
                .otp(otp)
                .expiresInSeconds(15 * 60)
                .build();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenOrOtp = request.getToken().trim();

        Optional<PasswordResetToken> tokenOpt = resetTokenRepository.findByTokenAndUsedFalse(tokenOrOtp);
        if (tokenOpt.isEmpty()) {
            tokenOpt = resetTokenRepository.findByOtpAndUsedFalse(tokenOrOtp);
        }

        PasswordResetToken resetToken = tokenOpt
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.INVALID_RESET_TOKEN));

        if (resetToken.isExpired()) {
            throw new BusinessRuleException(ErrorCode.RESET_TOKEN_EXPIRED);
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        log.info("[PASSWORD_RESET] Password successfully reset for user id={}, email={}", user.getId(), user.getEmail());
    }

    public JwtAuthenticationResponse refreshToken(String token) {
        if (!tokenProvider.validateToken(token)) {
            throw new BusinessRuleException(ErrorCode.UNAUTHORIZED);
        }

        String userId = tokenProvider.getUserIdFromJWT(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        String newJwt = tokenProvider.generateTokenFromUserId(userId);
        return new JwtAuthenticationResponse(newJwt, JwtAuthenticationResponse.UserDto.fromEntity(user));
    }

    @Transactional
    public JwtAuthenticationResponse loginWithGoogle(GoogleLoginRequest request) {
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
            User newUser = User.builder()
                    .email(finalEmail)
                    .name(finalName)
                    .avatar(finalPicture)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(User.Role.USER)
                    .isActive(true)
                    .joinedAt(LocalDateTime.now())
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            return userRepository.save(newUser);
        });

        if ((user.getAvatar() == null || user.getAvatar().isBlank()) && finalPicture != null && !finalPicture.isBlank()) {
            user.setAvatar(finalPicture);
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String jwt = tokenProvider.generateTokenFromUserId(user.getId());
        return new JwtAuthenticationResponse(jwt, JwtAuthenticationResponse.UserDto.fromEntity(user));
    }
}
