package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    long countByUserIdAndIsReadFalse(String userId);
    Page<Notification> findByUserId(String userId, Pageable pageable);
    Page<Notification> findByUserIdAndIsReadFalse(String userId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.id = :id and n.user.id = :userId")
    int markRead(@Param("id") String id, @Param("userId") String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.user.id = :userId and n.isRead = false")
    int markAllRead(@Param("userId") String userId);
}
