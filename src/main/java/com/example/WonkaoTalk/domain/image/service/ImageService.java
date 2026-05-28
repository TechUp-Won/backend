package com.example.WonkaoTalk.domain.image.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.image.dto.PresignedUrlRequest;
import com.example.WonkaoTalk.domain.image.dto.PresignedUrlResponse;
import com.example.WonkaoTalk.domain.product.event.ProductCreatedEvent;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

  private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
      "jpg", "image/jpeg",
      "jpeg", "image/jpeg",
      "png", "image/png",
      "webp", "image/webp"
  );

  private static final Pattern TEMP_KEY_PATTERN = Pattern.compile(
      "^temp/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|webp)$"
  );

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;

  @Value("${storage.endpoint}")
  private String endpoint;

  @Value("${storage.bucket}")
  private String bucket;

  @Value("${storage.temp-prefix}")
  private String tempPrefix;

  @Value("${storage.presigned-expiry-minutes}")
  private long expiryMinutes;

  public PresignedUrlResponse issuePresignedUrl(PresignedUrlRequest request) {
    String extension = extractExtension(request.filename());
    String contentType = ALLOWED_EXTENSIONS.get(extension);
    if (contentType == null) {
      throw new BusinessException(ErrorCode.IMAGE_INVALID_TYPE);
    }

    String objectKey = tempPrefix + UUID.randomUUID() + "." + extension;

    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(bucket)
        .key(objectKey)
        .contentType(contentType)
        .build();

    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(expiryMinutes))
        .putObjectRequest(putObjectRequest)
        .build();

    PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

    return new PresignedUrlResponse(
        presigned.url().toString(),
        objectKey,
        contentType
    );
  }

  public String validateAndGetProductUrl(String objectKey) {
    if (objectKey == null) {
      return null;
    }
    if (!TEMP_KEY_PATTERN.matcher(objectKey).matches()) {
      throw new BusinessException(ErrorCode.IMAGE_INVALID_KEY);
    }
    try {
      s3Client.headObject(HeadObjectRequest.builder()
          .bucket(bucket)
          .key(objectKey)
          .build());
    } catch (NoSuchKeyException e) {
      throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND_KEY);
    }
    String productKey = "products/" + objectKey.substring(tempPrefix.length());
    return endpoint + "/" + bucket + "/" + productKey;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleProductCreated(ProductCreatedEvent event) {
    event.objectKeys().forEach(objectKey -> {
      try {
        moveToProducts(objectKey);
      } catch (Exception e) {
        log.error("S3 파일 이동 실패 (재시도 필요): {}", objectKey, e);
      }
    });
  }

  private void moveToProducts(String objectKey) {
    String productKey = "products/" + objectKey.substring(tempPrefix.length());
    s3Client.copyObject(CopyObjectRequest.builder()
        .sourceBucket(bucket)
        .sourceKey(objectKey)
        .destinationBucket(bucket)
        .destinationKey(productKey)
        .build());
    s3Client.deleteObject(DeleteObjectRequest.builder()
        .bucket(bucket)
        .key(objectKey)
        .build());
  }

  public void deleteByUrl(String url) {
    String key = url.substring(endpoint.length() + 1 + bucket.length() + 1);
    s3Client.deleteObject(DeleteObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build());
  }

  private String extractExtension(String filename) {
    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == filename.length() - 1) {
      throw new BusinessException(ErrorCode.IMAGE_INVALID_TYPE);
    }
    return filename.substring(dotIndex + 1).toLowerCase();
  }
}
