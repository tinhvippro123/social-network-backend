package com.vietblog.application.service;

import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.infrastructure.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock AdminUserRepository users;
    @Mock AdminPostRepository posts;
    @Mock CommentRepository comments;
    @Mock ReportRepository reports;
    @Mock GroupRepository groups;
    @InjectMocks AdminService service;

    @Test
    void cannotBanAdministratorOrDemoteSelf() {
        User admin = User.builder().id("admin").role(User.Role.ADMIN).build();
        when(users.findLockedById("admin")).thenReturn(Optional.of(admin));
        assertThrows(BusinessRuleException.class, () -> service.setBanned("admin", "another-admin", true));
        assertThrows(BusinessRuleException.class, () -> service.changeRole("admin", "admin", User.Role.USER));
        verify(users, never()).save(any());
        assertTrue(admin.isActive());
        assertEquals(User.Role.ADMIN, admin.getRole());
    }

    @Test
    void banAndUnbanPersistAccessFlag() {
        User user = User.builder().id("user").role(User.Role.USER).build();
        when(users.findLockedById("user")).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        assertFalse(service.setBanned("user", "admin", true).active());
        assertTrue(service.setBanned("user", "admin", false).active());
        verify(users, times(2)).save(user);
    }

    @Test
    void rejectsUnboundedAndNegativePagination() {
        assertThrows(BusinessRuleException.class, () -> service.users(null, "", 0, 20));
        assertThrows(BusinessRuleException.class, () -> service.posts(null, "", 1, 101));
        verifyNoInteractions(users, posts);
    }
}
