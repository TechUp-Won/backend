package com.example.WonkaoTalk.domain.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.image.dto.PresignedUrlRequest;
import com.example.WonkaoTalk.domain.image.dto.PresignedUrlResponse;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

  @Mock
  private S3Presigner s3Presigner;

  @InjectMocks
  private ImageService imageService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(imageService, "bucket", "wonkao-talk");
    ReflectionTestUtils.setField(imageService, "tempPrefix", "temp/");
    ReflectionTestUtils.setField(imageService, "expiryMinutes", 15L);
  }

  // ── 성공 케이스 ────────────────────────────────────────────────────────────────

  @ParameterizedTest(name = "{0} → contentType: {1}")
  @CsvSource({
      "photo.jpg,  image/jpeg",
      "photo.jpeg, image/jpeg",
      "photo.png,  image/png",
      "photo.webp, image/webp",
  })
  @DisplayName("허용된 확장자는 올바른 contentType으로 Presigned URL을 발급한다")
  void issuesPresignedUrl_withCorrectContentType(String filename, String expectedContentType)
      throws Exception {
    mockPresigner("http://localhost:9000/wonkao-talk/temp/test.jpg");

    PresignedUrlResponse response = imageService.issuePresignedUrl(
        new PresignedUrlRequest(filename));

    assertThat(response.contentType()).isEqualTo(expectedContentType.trim());
  }

  @Test
  @DisplayName("objectKey는 temp/ 접두사와 UUID를 포함한다")
  void objectKey_startsWithTempPrefix() throws Exception {
    mockPresigner("http://localhost:9000/wonkao-talk/temp/test.jpg");

    PresignedUrlResponse response = imageService.issuePresignedUrl(
        new PresignedUrlRequest("shirt.jpg"));

    assertThat(response.objectKey()).startsWith("temp/");
    assertThat(response.objectKey()).endsWith(".jpg");
  }

  @Test
  @DisplayName("대문자 확장자도 허용된다")
  void uppercaseExtension_isAccepted() throws Exception {
    mockPresigner("http://localhost:9000/wonkao-talk/temp/test.jpg");

    PresignedUrlResponse response = imageService.issuePresignedUrl(
        new PresignedUrlRequest("photo.JPG"));

    assertThat(response.contentType()).isEqualTo("image/jpeg");
  }

  @Test
  @DisplayName("uploadUrl이 비어있지 않다")
  void uploadUrl_isNotBlank() throws Exception {
    mockPresigner("http://localhost:9000/wonkao-talk/temp/test.jpg?X-Amz-Signature=abc");

    PresignedUrlResponse response = imageService.issuePresignedUrl(
        new PresignedUrlRequest("photo.png"));

    assertThat(response.uploadUrl()).isNotBlank();
  }

  // ── 실패 케이스 ────────────────────────────────────────────────────────────────

  @ParameterizedTest(name = "파일명: {0}")
  @ValueSource(strings = {"photo.gif", "photo.bmp", "photo.tiff", "photo.svg"})
  @DisplayName("허용되지 않는 확장자는 IMAGE_INVALID_TYPE 예외를 던진다")
  void throwsException_whenExtensionNotAllowed(String filename) {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> imageService.issuePresignedUrl(new PresignedUrlRequest(filename)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.IMAGE_INVALID_TYPE);
  }

  @Test
  @DisplayName("확장자가 없는 파일명은 IMAGE_INVALID_TYPE 예외를 던진다")
  void throwsException_whenNoExtension() {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> imageService.issuePresignedUrl(new PresignedUrlRequest("photoWithoutExt")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.IMAGE_INVALID_TYPE);
  }

  @Test
  @DisplayName("점으로 끝나는 파일명은 IMAGE_INVALID_TYPE 예외를 던진다")
  void throwsException_whenFilenameEndsWithDot() {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> imageService.issuePresignedUrl(new PresignedUrlRequest("photo.")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.IMAGE_INVALID_TYPE);
  }

  // ── 헬퍼 ──────────────────────────────────────────────────────────────────────

  private void mockPresigner(String url) throws Exception {
    PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
    when(presigned.url()).thenReturn(URI.create(url).toURL());
    when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);
  }
}
