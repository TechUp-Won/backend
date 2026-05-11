package com.example.WonkaoTalk.common.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

  @Value("${storage.endpoint}")
  private String endpoint;

  @Value("${storage.access-key}")
  private String accessKey;

  @Value("${storage.secret-key}")
  private String secretKey;

  @Value("${storage.bucket}")
  private String bucket;

  @Value("${storage.region}")
  private String region;

  @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)))
        .region(Region.of(region))
        .forcePathStyle(true)
        .build();
  }

  @Bean
  public S3Presigner s3Presigner() {
    return S3Presigner.builder()
        .endpointOverride(URI.create(endpoint))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)))
        .region(Region.of(region))
        .serviceConfiguration(S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build())
        .build();
  }

  @Bean
  public CommandLineRunner bucketInitializer(S3Client s3Client) {
    return args -> {
      try {
        s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
      } catch (S3Exception e) {
        if (e.statusCode() == 404) {
          s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
      }
    };
  }
}
