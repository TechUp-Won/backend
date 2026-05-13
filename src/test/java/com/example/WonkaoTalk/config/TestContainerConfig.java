package com.example.WonkaoTalk.config;

import com.redis.testcontainers.RedisContainer;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
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

  @Bean
  public GenericContainer<?> minioContainer(ConfigurableEnvironment environment) {
    GenericContainer<?> minio = new GenericContainer<>("minio/minio:RELEASE.2025-09-07T16-13-09Z")
        .withEnv("MINIO_ROOT_USER", "minioadmin")
        .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
        .withCommand("server /data")
        .withExposedPorts(9000);

    minio.start();

    String endpoint = String.format("http://%s:%d", minio.getHost(), minio.getMappedPort(9000));

    Map<String, Object> properties = Map.of(
        "storage.endpoint", endpoint,
        "storage.access-key", "minioadmin",
        "storage.secret-key", "minioadmin",
        "storage.bucket", "wonkao-talk",
        "storage.region", "ap-northeast-2",
        "storage.temp-prefix", "temp/",
        "storage.presigned-expiry-minutes", 15
    );

    environment.getPropertySources().addFirst(new MapPropertySource("minio-test-properties", properties));

    return minio;
  }
}

