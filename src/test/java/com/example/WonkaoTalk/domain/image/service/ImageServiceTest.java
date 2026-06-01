package com.example.WonkaoTalk.domain.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.image.dto.PresignedUrlRequest;
import com.example.WonkaoTalk.domain.image.dto.PresignedUrlResponse;
import com.example.WonkaoTalk.domain.product.event.ProductCreatedEvent;
import java.net.URI;
import java.util.List;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

  @Mock
  private S3Client s3Client;

  @Mock
  private S3Presigner s3Presigner;

  @InjectMocks
  private ImageService imageService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(imageService, "endpoint", "http://localhost:9000");
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

  // ── validateAndGetProductUrl ──────────────────────────────────────────────────

  @Test
  @DisplayName("null objectKey는 null을 반환한다")
  void validateAndGetProductUrl_nullKey_returnsNull() {
    assertThat(imageService.validateAndGetProductUrl(null)).isNull();
  }

  @Test
  @DisplayName("temp/ 패턴에 맞지 않는 key는 IMAGE_INVALID_KEY 예외를 던진다")
  void validateAndGetProductUrl_invalidPattern_throwsImageInvalidKey() {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> imageService.validateAndGetProductUrl("products/some-file.jpg"));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.IMAGE_INVALID_KEY);
  }

  @Test
  @DisplayName("S3에 존재하지 않는 key는 IMAGE_NOT_FOUND_KEY 예외를 던진다")
  void validateAndGetProductUrl_keyNotFound_throwsImageNotFoundKey() {
    when(s3Client.headObject(argThat((HeadObjectRequest req) ->
        "wonkao-talk".equals(req.bucket())
        && "temp/550e8400-e29b-41d4-a716-446655440000.jpg".equals(req.key()))))
        .thenThrow(NoSuchKeyException.builder().build());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> imageService.validateAndGetProductUrl(
            "temp/550e8400-e29b-41d4-a716-446655440000.jpg"));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.IMAGE_NOT_FOUND_KEY);
  }

  @Test
  @DisplayName("유효한 temp key는 products 경로의 전체 URL을 반환한다")
  void validateAndGetProductUrl_validKey_returnsFullUrl() {
    when(s3Client.headObject(argThat((HeadObjectRequest req) ->
        "wonkao-talk".equals(req.bucket())
        && "temp/550e8400-e29b-41d4-a716-446655440000.jpg".equals(req.key()))))
        .thenReturn(HeadObjectResponse.builder().build());

    String result = imageService.validateAndGetProductUrl(
        "temp/550e8400-e29b-41d4-a716-446655440000.jpg");

    assertThat(result).isEqualTo(
        "http://localhost:9000/wonkao-talk/products/550e8400-e29b-41d4-a716-446655440000.jpg");
    verify(s3Client).headObject(argThat((HeadObjectRequest req) ->
        "wonkao-talk".equals(req.bucket())
        && "temp/550e8400-e29b-41d4-a716-446655440000.jpg".equals(req.key())));
  }

  // ── handleProductCreated ──────────────────────────────────────────────────────

  @Test
  @DisplayName("ProductCreatedEvent 수신 시 각 objectKey를 products 경로로 이동한다")
  void handleProductCreated_success_movesAllFiles() {
    when(s3Client.copyObject(any(CopyObjectRequest.class)))
        .thenReturn(CopyObjectResponse.builder().build());
    when(s3Client.deleteObject(argThat((DeleteObjectRequest req) ->
        req != null
        && "wonkao-talk".equals(req.bucket())
        && "temp/550e8400-e29b-41d4-a716-446655440000.jpg".equals(req.key()))))
        .thenReturn(DeleteObjectResponse.builder().build());
    when(s3Client.deleteObject(argThat((DeleteObjectRequest req) ->
        req != null
        && "wonkao-talk".equals(req.bucket())
        && "temp/661f9511-f3ac-52e5-b827-557766551111.png".equals(req.key()))))
        .thenReturn(DeleteObjectResponse.builder().build());

    imageService.handleProductCreated(new ProductCreatedEvent(
        List.of("temp/550e8400-e29b-41d4-a716-446655440000.jpg",
            "temp/661f9511-f3ac-52e5-b827-557766551111.png")));

    verify(s3Client).copyObject(argThat((CopyObjectRequest req) ->
        "wonkao-talk".equals(req.sourceBucket())
        && "temp/550e8400-e29b-41d4-a716-446655440000.jpg".equals(req.sourceKey())
        && "wonkao-talk".equals(req.destinationBucket())
        && "products/550e8400-e29b-41d4-a716-446655440000.jpg".equals(req.destinationKey())));
    verify(s3Client).copyObject(argThat((CopyObjectRequest req) ->
        "wonkao-talk".equals(req.sourceBucket())
        && "temp/661f9511-f3ac-52e5-b827-557766551111.png".equals(req.sourceKey())
        && "wonkao-talk".equals(req.destinationBucket())
        && "products/661f9511-f3ac-52e5-b827-557766551111.png".equals(req.destinationKey())));
    verify(s3Client).deleteObject(argThat((DeleteObjectRequest req) ->
        "wonkao-talk".equals(req.bucket())
        && "temp/550e8400-e29b-41d4-a716-446655440000.jpg".equals(req.key())));
    verify(s3Client).deleteObject(argThat((DeleteObjectRequest req) ->
        "wonkao-talk".equals(req.bucket())
        && "temp/661f9511-f3ac-52e5-b827-557766551111.png".equals(req.key())));
  }

  @Test
  @DisplayName("파일 이동 중 예외가 발생해도 나머지 파일 처리를 계속한다")
  void handleProductCreated_moveThrows_continuesOtherFiles() {
    when(s3Client.copyObject(argThat((CopyObjectRequest req) ->
        req != null
        && "wonkao-talk".equals(req.sourceBucket())
        && "temp/550e8400-e29b-41d4-a716-446655440000.jpg".equals(req.sourceKey()))))
        .thenThrow(new RuntimeException("S3 error"));
    when(s3Client.copyObject(argThat((CopyObjectRequest req) ->
        req != null
        && "wonkao-talk".equals(req.sourceBucket())
        && "temp/661f9511-f3ac-52e5-b827-557766551111.png".equals(req.sourceKey()))))
        .thenThrow(new RuntimeException("S3 error"));

    assertDoesNotThrow(() ->
        imageService.handleProductCreated(new ProductCreatedEvent(
            List.of("temp/550e8400-e29b-41d4-a716-446655440000.jpg",
                "temp/661f9511-f3ac-52e5-b827-557766551111.png"))));

    verify(s3Client).copyObject(argThat((CopyObjectRequest req) ->
        "wonkao-talk".equals(req.sourceBucket())
        && "temp/550e8400-e29b-41d4-a716-446655440000.jpg".equals(req.sourceKey())
        && "wonkao-talk".equals(req.destinationBucket())
        && "products/550e8400-e29b-41d4-a716-446655440000.jpg".equals(req.destinationKey())));
    verify(s3Client).copyObject(argThat((CopyObjectRequest req) ->
        "wonkao-talk".equals(req.sourceBucket())
        && "temp/661f9511-f3ac-52e5-b827-557766551111.png".equals(req.sourceKey())
        && "wonkao-talk".equals(req.destinationBucket())
        && "products/661f9511-f3ac-52e5-b827-557766551111.png".equals(req.destinationKey())));
    verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
  }

  // ── deleteByUrl ───────────────────────────────────────────────────────────────

  @Test
  @DisplayName("null URL은 아무 작업 없이 반환한다")
  void deleteByUrl_nullUrl_returnsWithoutException() {
    assertDoesNotThrow(() -> imageService.deleteByUrl(null));
    verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
  }

  @Test
  @DisplayName("endpoint/bucket 접두사가 없는 URL은 IMAGE_INVALID_KEY 예외를 던진다")
  void deleteByUrl_invalidPrefix_throwsImageInvalidKey() {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> imageService.deleteByUrl("http://other-host/wonkao-talk/products/file.jpg"));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.IMAGE_INVALID_KEY);
  }

  @Test
  @DisplayName("유효한 URL은 해당 S3 객체를 삭제한다")
  void deleteByUrl_validUrl_callsDeleteObject() {
    when(s3Client.deleteObject(argThat((DeleteObjectRequest req) ->
        req != null
        && "wonkao-talk".equals(req.bucket())
        && "products/550e8400-e29b-41d4-a716-446655440000.jpg".equals(req.key()))))
        .thenReturn(DeleteObjectResponse.builder().build());

    imageService.deleteByUrl(
        "http://localhost:9000/wonkao-talk/products/550e8400-e29b-41d4-a716-446655440000.jpg");

    verify(s3Client).deleteObject(argThat((DeleteObjectRequest req) ->
        "wonkao-talk".equals(req.bucket())
        && "products/550e8400-e29b-41d4-a716-446655440000.jpg".equals(req.key())));
  }

  // ── 헬퍼 ──────────────────────────────────────────────────────────────────────

  private void mockPresigner(String url) throws Exception {
    PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
    when(presigned.url()).thenReturn(URI.create(url).toURL());
    when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);
  }
}
