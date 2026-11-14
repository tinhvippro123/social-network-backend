package com.vietblog.application.service;

import com.vietblog.application.port.CacheGateway;
import com.vietblog.domain.entity.SystemSetting;
import com.vietblog.infrastructure.repository.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Quản lý cấu hình hệ thống. Chỉ chấp nhận các key đã khai báo trong DEFAULTS,
 * giá trị được ép về đúng kiểu (boolean / số / chuỗi) theo giá trị mặc định.
 */
@Service
public class SystemSettingsService {
    public static final String SETTINGS_CACHE_KEY = "vietblog:cache:system:settings";
    private static final Duration L1_TTL = Duration.ofSeconds(15);
    private static final Duration REDIS_TTL = Duration.ofSeconds(60);
    private static final Map<String, Object> DEFAULTS;

    static {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("siteName", "VietBlog");
        d.put("siteDescription", "Nền tảng chia sẻ kiến thức cho cộng đồng");
        d.put("allowRegistration", true);
        d.put("requireEmailVerification", true);
        d.put("autoHideReportThreshold", 10);
        d.put("enableRealTimeChat", true);
        d.put("enableLocationPosts", true);
        d.put("enableSemanticSearch", true);
        d.put("maintenanceMode", false);
        DEFAULTS = Map.copyOf(d);
    }

    private record CachedSettings(Map<String, Object> values, long expiresAtMillis) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }

    private final SystemSettingRepository repository;
    private final CacheGateway cache;
    private final ObjectMapper json;
    private volatile CachedSettings cached;

    public SystemSettingsService(SystemSettingRepository repository) {
        this(repository, CacheGateway.NOOP, JsonMapper.builder().build());
    }

    @Autowired
    public SystemSettingsService(SystemSettingRepository repository, CacheGateway cache, ObjectMapper json) {
        this.repository = repository;
        this.cache = cache != null ? cache : CacheGateway.NOOP;
        this.json = json != null ? json : JsonMapper.builder().build();
    }

    public Map<String, Object> getAll() {
        CachedSettings current = this.cached;
        if (current != null && !current.isExpired()) {
            return new LinkedHashMap<>(current.values());
        }
        Optional<String> cachedJson = cache.get(SETTINGS_CACHE_KEY);
        if (cachedJson.isPresent()) {
            try {
                Map<String, Object> raw = json.readValue(cachedJson.get(), new TypeReference<>() {});
                Map<String, Object> normalized = normalizeSettingsMap(raw);
                this.cached = new CachedSettings(
                        Collections.unmodifiableMap(normalized),
                        System.currentTimeMillis() + L1_TTL.toMillis());
                return new LinkedHashMap<>(normalized);
            } catch (RuntimeException ignored) {
                cache.evict(SETTINGS_CACHE_KEY);
            }
        }
        Map<String, Object> fresh = loadFromDatabase();
        refreshCaches(fresh);
        return new LinkedHashMap<>(fresh);
    }

    public boolean isMaintenanceMode() {
        return getBoolean("maintenanceMode");
    }

    public boolean isRequireEmailVerification() {
        return getBoolean("requireEmailVerification");
    }

    public boolean isAllowRegistration() {
        return getBoolean("allowRegistration");
    }

    public boolean getBoolean(String key) {
        Object value = getAll().get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.TRUE.equals(DEFAULTS.get(key));
    }

    @Transactional
    public Map<String, Object> update(Map<String, Object> changes) {
        if (changes != null) {
            Map<String, String> normalized = new LinkedHashMap<>();
            changes.forEach((key, value) -> {
                if (!DEFAULTS.containsKey(key) || value == null) return;
                String text = String.valueOf(value).trim();
                if (text.length() > 2000) text = text.substring(0, 2000);
                normalized.put(key, text);
            });
            if (!normalized.isEmpty()) {
                Map<String, SystemSetting> existing = new LinkedHashMap<>();
                repository.findAllById(normalized.keySet()).forEach(s -> existing.put(s.getKey(), s));
                List<SystemSetting> toSave = new ArrayList<>(normalized.size());
                normalized.forEach((key, text) -> {
                    SystemSetting setting = existing.get(key);
                    if (setting == null) {
                        toSave.add(new SystemSetting(key, text));
                    } else {
                        setting.setValue(text);
                        toSave.add(setting);
                    }
                });
                repository.saveAll(toSave);
            }
        }
        Map<String, Object> fresh = loadFromDatabase();
        refreshCaches(fresh);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        invalidateCaches();
                    }
                }
            });
        }
        return new LinkedHashMap<>(fresh);
    }

    private Map<String, Object> loadFromDatabase() {
        Map<String, Object> result = new LinkedHashMap<>(DEFAULTS);
        repository.findAll().forEach(s -> {
            if (DEFAULTS.containsKey(s.getKey())) {
                result.put(s.getKey(), convert(DEFAULTS.get(s.getKey()), s.getValue()));
            }
        });
        return result;
    }

    private Map<String, Object> normalizeSettingsMap(Map<String, Object> raw) {
        Map<String, Object> result = new LinkedHashMap<>(DEFAULTS);
        if (raw != null) {
            raw.forEach((key, value) -> {
                if (DEFAULTS.containsKey(key) && value != null) {
                    result.put(key, convert(DEFAULTS.get(key), String.valueOf(value)));
                }
            });
        }
        return result;
    }

    private void refreshCaches(Map<String, Object> fresh) {
        this.cached = new CachedSettings(
                Collections.unmodifiableMap(new LinkedHashMap<>(fresh)),
                System.currentTimeMillis() + L1_TTL.toMillis());
        try {
            cache.put(SETTINGS_CACHE_KEY, json.writeValueAsString(fresh), REDIS_TTL);
        } catch (RuntimeException ignored) {
            // Redis offline or serialization failure must not break settings reads/updates.
        }
    }

    private void invalidateCaches() {
        this.cached = null;
        cache.evict(SETTINGS_CACHE_KEY);
    }

    private Object convert(Object defaultValue, String raw) {
        if (raw == null) return defaultValue;
        if (defaultValue instanceof Boolean) return Boolean.parseBoolean(raw);
        if (defaultValue instanceof Integer) {
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return raw;
    }
}
