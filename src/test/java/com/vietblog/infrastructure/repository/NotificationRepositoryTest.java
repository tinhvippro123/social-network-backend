package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.Notification;
import com.vietblog.domain.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "spring.datasource.url=jdbc:h2:mem:admin_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationRepositoryTest {
    @Autowired EntityManager entityManager;
    @Autowired NotificationRepository notifications;

    @Test
    void notificationUpdatesCannotCrossRecipientBoundary() {
        User recipient = persistUser("recipient@example.test");
        User outsider = persistUser("outsider@example.test");
        Notification own = notifications.save(Notification.builder().user(recipient).type(Notification.NotificationType.SYSTEM).message("Own").build());
        Notification other = notifications.save(Notification.builder().user(outsider).type(Notification.NotificationType.SYSTEM).message("Other").build());
        entityManager.flush();

        assertEquals(0, notifications.markRead(own.getId(), outsider.getId()));
        assertFalse(notifications.findById(own.getId()).orElseThrow().isRead());
        assertEquals(1, notifications.markAllRead(recipient.getId()));
        assertTrue(notifications.findById(own.getId()).orElseThrow().isRead());
        assertFalse(notifications.findById(other.getId()).orElseThrow().isRead());
        assertEquals(0, notifications.countByUserIdAndIsReadFalse(recipient.getId()));
        assertEquals(1, notifications.countByUserIdAndIsReadFalse(outsider.getId()));
    }

    private User persistUser(String email) {
        User user = User.builder().email(email).password("unused-test-password").name(email).build();
        entityManager.persist(user);
        return user;
    }
}
