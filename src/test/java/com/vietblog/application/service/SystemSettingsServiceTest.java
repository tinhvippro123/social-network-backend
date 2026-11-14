package com.vietblog.application.service;

import com.vietblog.application.port.CacheGateway;
import com.vietblog.domain.entity.SystemSetting;
import com.vietblog.infrastructure.repository.SystemSettingRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemSettingsServiceTest {

    @Test
    void cachesSettingsInMemoryAndRedisAndRefreshesImmediatelyOnUpdate() {
        SystemSettingRepository repository = mock(SystemSettingRepository.class);
        CacheGateway cache = mock(CacheGateway.class);
        var json = JsonMapper.builder().build();

        when(cache.get(SystemSettingsService.SETTINGS_CACHE_KEY)).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(List.of(
                new SystemSetting("maintenanceMode", "false"),
                new SystemSetting("autoHideReportThreshold", "12")
        ));

        SystemSettingsService service = new SystemSettingsService(repository, cache, json);

        assertThat(service.isMaintenanceMode()).isFalse();
        assertThat(service.isAllowRegistration()).isTrue();
        assertThat(service.isRequireEmailVerification()).isTrue();
        assertThat(service.getAll().get("autoHideReportThreshold")).isEqualTo(12);

        // Repeated reads hit L1 cache without additional repository or Redis queries
        verify(repository, times(1)).findAll();
        verify(cache, times(1)).get(SystemSettingsService.SETTINGS_CACHE_KEY);
        verify(cache, times(1)).put(eq(SystemSettingsService.SETTINGS_CACHE_KEY), anyString(), any(Duration.class));

        // Update persists in batch and immediately refreshes cached values
        SystemSetting existing = new SystemSetting("maintenanceMode", "false");
        when(repository.findAllById(any())).thenReturn(List.of(existing));
        when(repository.findAll()).thenReturn(List.of(
                new SystemSetting("maintenanceMode", "true"),
                new SystemSetting("autoHideReportThreshold", "12")
        ));

        Map<String, Object> updated = service.update(Map.of("maintenanceMode", true));
        assertThat(updated.get("maintenanceMode")).isEqualTo(true);
        assertThat(service.isMaintenanceMode()).isTrue();

        verify(repository, times(1)).saveAll(anyList());
        verify(repository, times(2)).findAll();
    }
}
