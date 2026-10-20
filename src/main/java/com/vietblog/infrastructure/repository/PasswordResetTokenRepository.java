package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.PasswordResetToken;
import com.vietblog.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);
    Optional<PasswordResetToken> findByOtpAndUsedFalse(String otp);
    Optional<PasswordResetToken> findFirstByUserAndUsedFalseOrderByExpiryDateDesc(User user);
}
