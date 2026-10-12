package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM User u JOIN u.followers f WHERE u.id = :targetUserId AND f.id = :currentUserId")
    boolean isFollowing(@Param("targetUserId") String targetUserId, @Param("currentUserId") String currentUserId);

    @Query("SELECT f FROM User u JOIN u.followers f WHERE u.id = :userId")
    List<User> findFollowersByUserId(@Param("userId") String userId);

    @Query("SELECT f FROM User u JOIN u.following f WHERE u.id = :userId")
    List<User> findFollowingByUserId(@Param("userId") String userId);

    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.followersCount DESC")
    List<User> findTopUsers(Pageable pageable);
}
