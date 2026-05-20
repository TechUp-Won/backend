package com.example.WonkaoTalk.domain.image.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.image.dto.PresignedUrlRequest;
import com.example.WonkaoTalk.domain.image.dto.PresignedUrlResponse;
import com.example.WonkaoTalk.domain.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/images")
@RequiredArgsConstructor
@Tag(name = "이미지", description = "이미지 업로드용 Presigned URL 발급 API")
public class ImageController {

  private final ImageService imageService;

  @Operation(summary = "Presigned URL 발급", description = "이미지 업로드를 위한 S3/MinIO Presigned URL을 발급합니다.")
  @PostMapping("/presigned-url")
  public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
      @Valid @RequestBody PresignedUrlRequest request
  ) {
    PresignedUrlResponse data = imageService.issuePresignedUrl(request);
    return ResponseEntity.ok(ApiResponse.success("Presigned URL이 발급되었습니다.", data));
  }
}
