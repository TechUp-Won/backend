package com.example.WonkaoTalk.common.exception;

import com.example.WonkaoTalk.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  // 우리가 정의한 BusinessException이 발생했을 때
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
    ErrorCode errorCode = e.getErrorCode();
    return ResponseEntity
        .status(errorCode.getHttpStatus())
        .body(ApiResponse.error(errorCode.getCode(), e.getMessage()));
  }

  // @Valid 검증 실패 예외 처리
  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<ApiResponse<Void>> handleValidationException(
      MethodArgumentNotValidException e) {
    FieldError fieldError = e.getBindingResult().getFieldError();
    String field = fieldError != null ? fieldError.getField() : "";
    String errorMessage = fieldError != null ?
        fieldError.getDefaultMessage() : "입력값이 올바르지 않습니다";

    ErrorCode errorCode = ErrorCode.BAD_REQUEST;

    // 예외 필드가 "email" 인 경우 나중에 동일한 필드에서 발생하는 에러가 존재할 경우 에러 메세지 검토 필요.
    if ("email".equals(field)) {
      errorCode = ErrorCode.AUTH_INVALID_EMAIL;
    } else if ("password".equals(field)) {
      errorCode = ErrorCode.AUTH_INVALID_PASSWORD;
    }

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(errorCode.getCode(), errorMessage));
  }

  // 그 외 예상치 못한 에러가 발생했을 때 (500 에러 포장)
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    return ResponseEntity
        .status(500)
        .body(ApiResponse.error("SYS-INTERNAL-ERROR", "서버 내부 오류가 발생했습니다."));
  }
}
