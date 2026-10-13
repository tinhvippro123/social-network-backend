package com.vietblog.application.service;

import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.infrastructure.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository notifications;
    @InjectMocks NotificationService service;

    @Test
    void cannotMarkAnotherRecipientsNotificationRead() {
        when(notifications.markRead("notification", "attacker")).thenReturn(0);
        assertThrows(ResourceNotFoundException.class, () -> service.markRead("notification", "attacker"));
        verify(notifications).markRead("notification", "attacker");
        verifyNoMoreInteractions(notifications);
    }

    @Test
    void markAllReadIsScopedToCurrentRecipient() {
        when(notifications.markAllRead("recipient")).thenReturn(3);
        assertEquals(3, service.markAllRead("recipient"));
        verify(notifications).markAllRead("recipient");
        verifyNoMoreInteractions(notifications);
    }
}
