package com.example.WonkaoTalk.domain.image.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.image.dto.PresignedUrlRequest;
import com.example.WonkaoTalk.domain.image.dto.PresignedUrlResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class ImageService {

  private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
      "jpg", "image/jpeg",
      "jpeg", "image/jpeg",
      "png", "image/png",
      "webp", "image/webp"
  );

  private final S3Presigner s3Presigner;

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

  private String extractExtension(String filename) {
    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == filename.length() - 1) {
      throw new BusinessException(ErrorCode.IMAGE_INVALID_TYPE);
    }
    return filename.substring(dotIndex + 1).toLowerCase();
  }
}
