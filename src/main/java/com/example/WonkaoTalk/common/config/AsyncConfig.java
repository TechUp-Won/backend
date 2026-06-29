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
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.initialize();
    return executor;
  }

  /**
   * BCrypt 연산(로그인 검증, 회원가입 해싱)을 위한 벌크헤드 스레드풀. 톰캣 워커 스레드가 BCrypt 연산으로 선점되지 않도록 별도 풀로 분리하고, CPU 코어의 약
   * 65%만 점유하도록 고정 크기로 제한해 다른 API 처리용 CPU 자원을 남겨둔다. >> 80%까지 상향 수정 core == max 로 고정해 풀이 늘어나며 CPU를 추가
   * 점유하지 않게 한다. 로그인/회원가입 모두 같은 CPU 예산을 공유하므로 풀을 분리하지 않고 함께 사용한다. 대기 큐의 공간 대폭 감축 계산식: (스레드 풀 크기 /
   * BCrypt 1회 소요 시간) * 허용 대기 시간 = (8 / 300ms) * 1000ms = 27 -> 30으로 설정 errorRate 폭등으로 인한 여유 공간 필요로
   * 3배인 90으로 설정
   */
  @Bean(name = "bcryptExecutor")
  public Executor bcryptExecutor() {
    int cores = Runtime.getRuntime().availableProcessors();
    int poolSize = Math.max(2, (int) Math.round(cores * 0.8));

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(poolSize);
    executor.setMaxPoolSize(poolSize);
    executor.setQueueCapacity(90); // 큐 초과 시 TaskRejectedException -> 503 처리
    executor.setThreadNamePrefix("BCryptAsync-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.initialize();
    return executor;
  }
}
