package org.project.floodalert.ingestion.controller;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import org.project.floodalert.ingestion.service.impl.SensorBlacklistServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class CacheMonitoringController {
    private final SensorBlacklistServiceImpl sensorBlacklistService;

    @GetMapping("/blacklist/stats")
    public Map<String, Object> getBlacklistCacheStats() {
        CacheStats stats = sensorBlacklistService.getCacheStats();
        return Map.of(
                "hitCount", stats.hitCount(),
                "missCount", stats.missCount(),
                "hitRate", stats.hitRate(),
                "evictionCount", stats.evictionCount(),
                "loadSuccessCount", stats.loadSuccessCount(),
                "loadFailureCount", stats.loadFailureCount()
        );
    }

    @PostMapping("/blacklist/clear")
    public ResponseEntity<String> clearCache() {
        sensorBlacklistService.clearCache();
        return ResponseEntity.ok("Cache cleared successfully");
    }
}
