package com.example.WonkaoTalk.config;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainerConfig {

  @Bean
  @ServiceConnection
  public PostgreSQLContainer postgreSQLContainer() {
    return new PostgreSQLContainer("postgres:17");
  }

  @Bean
  @ServiceConnection
  public RedisContainer redisContainer() {
    return new RedisContainer("redis:7.4");
  }

  @Bean
  @ServiceConnection
  public KafkaContainer kafkaContainer() {
    return new KafkaContainer("apache/kafka:4.0.0");
  }
}
