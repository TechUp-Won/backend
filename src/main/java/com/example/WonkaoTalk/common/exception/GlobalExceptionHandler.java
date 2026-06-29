package com.example.WonkaoTalk.common.exception;

import com.example.WonkaoTalk.common.response.ApiResponse;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), errorMessage));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  protected ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException e) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), "요청 형식이 올바르지 않습니다"));
  }

  // 벌크헤드 스레드풀(bcryptExecutor 등)의 큐가 가득 차 작업을 거부했을 때
  @ExceptionHandler(TaskRejectedException.class)
  protected ResponseEntity<ApiResponse<Void>> handleTaskRejectedException(
      TaskRejectedException e) {
    ErrorCode errorCode = ErrorCode.SERVICE_UNAVAILABLE;
    return ResponseEntity
        .status(errorCode.getHttpStatus())
        .body(ApiResponse.error(errorCode.getCode(), "요청이 많아 처리할 수 없습니다. 잠시 후 다시 시도해 주세요."));
  }

  // 그 외 예상치 못한 에러가 발생했을 때 (500 에러 포장)
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    return ResponseEntity
        .status(500)
        .body(ApiResponse.error("SYS-INTERNAL-ERROR", "서버 내부 오류가 발생했습니다."));
  }
}
