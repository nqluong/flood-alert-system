package org.project.floodalert.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "auditLogExecutor")
    public Executor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size
        executor.setCorePoolSize(2);

        // Maximum pool size
        executor.setMaxPoolSize(5);

        // Queue capacity
        executor.setQueueCapacity(100);

        // Thread name prefix
        executor.setThreadNamePrefix("audit-log-");

        // Rejection policy when queue is full
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Audit log executor initialized with core pool size: {}, max pool size: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }

    @Bean(name = "cacheExecutor")
    public Executor cacheExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("cache-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("Cache executor initialized with core pool size: {}, max pool size: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }
}
