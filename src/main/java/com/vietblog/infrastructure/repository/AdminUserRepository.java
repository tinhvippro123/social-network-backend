package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<User, String> {
    @Query("select u from User u where (:role is null or u.role = :role) " +
            "and (lower(u.name) like lower(concat('%', :search, '%')) " +
            "or lower(u.email) like lower(concat('%', :search, '%')))")
    Page<User> search(@Param("role") User.Role role, @Param("search") String search, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findLockedById(@Param("id") String id);

    long countByJoinedAtGreaterThanEqualAndJoinedAtLessThan(LocalDateTime from, LocalDateTime to);

    java.util.List<User> findTop5ByOrderByJoinedAtDesc();
}
