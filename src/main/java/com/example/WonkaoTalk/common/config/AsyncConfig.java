package com.example.WonkaoTalk.common.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean(name = "revocationExecutor")
  public Executor revocationExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10); // 기본 대기 스레드
    executor.setMaxPoolSize(50);  // 최대 확장 스레드
    executor.setQueueCapacity(100); // 큐 대기열
    executor.setThreadNamePrefix("RevocationAsync-");
    executor.initialize();
    return executor;
  }
}
