package com.vietblog.application.service;

import com.vietblog.domain.entity.SystemSetting;
import com.vietblog.infrastructure.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Quản lý cấu hình hệ thống. Chỉ chấp nhận các key đã khai báo trong DEFAULTS,
 * giá trị được ép về đúng kiểu (boolean / số / chuỗi) theo giá trị mặc định.
 */
@Service
@RequiredArgsConstructor
public class SystemSettingsService {
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

    private final SystemSettingRepository repository;

    @Transactional(readOnly = true)
    public Map<String, Object> getAll() {
        Map<String, Object> result = new LinkedHashMap<>(DEFAULTS);
        repository.findAll().forEach(s -> {
            if (DEFAULTS.containsKey(s.getKey())) {
                result.put(s.getKey(), convert(DEFAULTS.get(s.getKey()), s.getValue()));
            }
        });
        return result;
    }

    @Transactional
    public Map<String, Object> update(Map<String, Object> changes) {
        if (changes != null) {
            changes.forEach((key, value) -> {
                if (!DEFAULTS.containsKey(key) || value == null) return;
                String text = String.valueOf(value).trim();
                if (text.length() > 2000) text = text.substring(0, 2000);
                repository.save(new SystemSetting(key, text));
            });
        }
        return getAll();
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
