package com.vietblog.infrastructure.security;

import com.vietblog.domain.entity.User;
import com.vietblog.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceCacheTest {

    @Test
    void cachesUserPrincipalAndEvictsImmediatelyWhenUserChanges() {
        UserRepository repository = mock(UserRepository.class);
        User activeUser = User.builder()
                .id("u-1")
                .email("user@vietblog.test")
                .password("encoded")
                .role(User.Role.USER)
                .isActive(true)
                .build();
        when(repository.findById("u-1")).thenReturn(Optional.of(activeUser));

        CustomUserDetailsService service = new CustomUserDetailsService(repository);
        service.registerCacheInvalidation();
        try {
            UserDetails first = service.loadUserById("u-1");
            UserDetails second = service.loadUserById("u-1");
            assertThat(first.isEnabled()).isTrue();
            assertThat(second.isEnabled()).isTrue();
            verify(repository, times(1)).findById("u-1");

            User bannedUser = User.builder()
                    .id("u-1")
                    .email("user@vietblog.test")
                    .password("encoded")
                    .role(User.Role.USER)
                    .isActive(false)
                    .build();
            when(repository.findById("u-1")).thenReturn(Optional.of(bannedUser));

            User.CacheInvalidationListener.notifyChanged("u-1");

            UserDetails afterBan = service.loadUserById("u-1");
            assertThat(afterBan.isEnabled()).isFalse();
            verify(repository, times(2)).findById("u-1");
        } finally {
            service.unregisterCacheInvalidation();
        }
    }
}
