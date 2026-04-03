package org.project.floodalert.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
@ConditionalOnProperty(
        name = "spring.threads.virtual.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class VirtualThreadSchedulingConfig {

    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        log.info("Creating Virtual Thread Executor bean");
        log.info("Virtual Threads: Using Project Loom for lightweight concurrency");
        log.info("Virtual Thread support is ENABLED globally via spring.threads.virtual.enabled=true");

        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
