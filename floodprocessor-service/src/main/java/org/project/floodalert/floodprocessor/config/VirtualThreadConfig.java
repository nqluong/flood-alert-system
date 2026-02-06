package org.project.floodalert.floodprocessor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
@Configuration
@EnableAsync
public class VirtualThreadConfig {
    
    /**
     * Cấu hình AsyncTaskExecutor sử dụng Virtual Threads
     */
    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor asyncTaskExecutor() {
        log.info("Khởi tạo Virtual Thread Executor cho Spring Boot application");

        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("vt-");
        executor.setVirtualThreads(true);
        executor.setTaskDecorator(runnable -> {
            return runnable;
        });
        
        log.info("Virtual Thread Executor đã được cấu hình thành công");
        return executor;
    }
    

    @Bean(name = "virtualThreadExecutor")
    public ExecutorService virtualThreadExecutor() {
        log.info("Khởi tạo Virtual Thread ExecutorService");
        // Tạo executor sử dụng Virtual Threads từ Java 21
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
