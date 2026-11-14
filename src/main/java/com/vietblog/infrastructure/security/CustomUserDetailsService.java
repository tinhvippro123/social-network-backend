package com.vietblog.infrastructure.security;

import com.vietblog.domain.entity.User;
import com.vietblog.infrastructure.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final Duration PRINCIPAL_TTL = Duration.ofSeconds(15);
    private static final int MAX_CACHE_ENTRIES = 5000;

    private record CachedPrincipal(UserPrincipal principal, long expiresAtMillis) {
        boolean isExpired(long nowMillis) {
            return nowMillis > expiresAtMillis;
        }
    }

    private final UserRepository userRepository;
    private final ConcurrentMap<String, CachedPrincipal> principalCache = new ConcurrentHashMap<>();
    private volatile AutoCloseable invalidationRegistration;

    @PostConstruct
    void registerCacheInvalidation() {
        invalidationRegistration = User.CacheInvalidationListener.register(this::onUserEntityChanged);
    }

    @PreDestroy
    void unregisterCacheInvalidation() {
        AutoCloseable reg = invalidationRegistration;
        invalidationRegistration = null;
        if (reg != null) {
            try {
                reg.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        UserPrincipal principal = UserPrincipal.create(user);
        cachePrincipal(principal);
        return principal;
    }

    public UserDetails loadUserById(String id) {
        if (id != null) {
            long now = System.currentTimeMillis();
            CachedPrincipal cached = principalCache.get(id);
            if (cached != null) {
                if (!cached.isExpired(now)) {
                    return cached.principal();
                }
                principalCache.remove(id, cached);
            }
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        UserPrincipal principal = UserPrincipal.create(user);
        cachePrincipal(principal);
        return principal;
    }

    public void evictUser(String id) {
        if (id != null) {
            principalCache.remove(id);
        }
    }

    private void cachePrincipal(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) return;
        long now = System.currentTimeMillis();
        if (principalCache.size() >= MAX_CACHE_ENTRIES) {
            principalCache.entrySet().removeIf(e -> e.getValue().isExpired(now));
            if (principalCache.size() >= MAX_CACHE_ENTRIES) {
                principalCache.clear();
            }
        }
        principalCache.put(principal.getId(), new CachedPrincipal(principal, now + PRINCIPAL_TTL.toMillis()));
    }

    private void onUserEntityChanged(String userId) {
        evictUser(userId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            try {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        evictUser(userId);
                    }
                });
            } catch (IllegalStateException ignored) {
            }
        }
    }
}
