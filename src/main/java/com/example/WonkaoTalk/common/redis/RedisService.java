package com.example.WonkaoTalk.common.redis;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

  // Spring Boot 설정 RedisTemplate 주입
  private final RedisTemplate<String, String> redisTemplate;

  public void setValues(String key, String data, Duration duration) {
    redisTemplate.opsForValue().set(key, data, duration);
  }

  public String getValues(String key) {
    return redisTemplate.opsForValue().get(key);
  }

  public void deleteValues(String key) {
    redisTemplate.delete(key);
  }

  public boolean hasKey(String key) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }
}
