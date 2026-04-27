package com.example.WonkaoTalk.common.exception;

import com.example.WonkaoTalk.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
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

  // 그 외 예상치 못한 에러가 발생했을 때 (500 에러 포장)
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    return ResponseEntity
        .status(500)
        .body(ApiResponse.error("SYS-INTERNAL-ERROR", "서버 내부 오류가 발생했습니다."));
  }
}
