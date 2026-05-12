package org.project.floodalert.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.service.StaticLocationRedisService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStartupSync {
    
    private final StaticLocationRedisService staticLocationRedisService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncAddressesOnStartup() {
        log.info("========== BẮT ĐẦU SYNC ADDRESSES LÊN REDIS ==========");
        
        try {
            long syncCount = staticLocationRedisService.syncAllAddressesToRedis();
            
            log.info("========== HOÀN TẤT SYNC ADDRESSES ==========");
            log.info("Đã sync {} địa chỉ lên Redis key: user:static_locations", syncCount);
            
        } catch (Exception e) {
            log.error("========== LỖI KHI SYNC ADDRESSES ==========", e);
            log.error("Application vẫn tiếp tục chạy nhưng Redis có thể chưa có data đầy đủ");
        }
    }
}
