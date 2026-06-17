package com.example.WonkaoTalk.common.config;

import com.example.WonkaoTalk.domain.product.dto.ProductDetailCacheDto;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

@Configuration
@EnableCaching
public class RedisCacheConfig {

  @Value("${cache.redis.host}")
  private String cacheRedisHost;

  @Value("${cache.redis.port}")
  private int cacheRedisPort;

  @Bean
  public RedisCacheManager cacheManager() {
    LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
        new RedisStandaloneConfiguration(cacheRedisHost, cacheRedisPort));
    connectionFactory.afterPropertiesSet();
    ObjectMapper om = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);

    RedisSerializer<ProductDetailCacheDto> serializer = new RedisSerializer<>() {
      @Override
      public byte[] serialize(ProductDetailCacheDto value) throws SerializationException {
        if (value == null) return new byte[0];
        try {
          return om.writeValueAsBytes(value);
        } catch (Exception e) {
          throw new SerializationException("JSON serialization error", e);
        }
      }

      @Override
      public ProductDetailCacheDto deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) return null;
        try {
          return om.readValue(bytes, ProductDetailCacheDto.class);
        } catch (Exception e) {
          throw new SerializationException("JSON deserialization error", e);
        }
      }
    };

    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(30))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(serializer));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(config)
        .build();
  }
}
