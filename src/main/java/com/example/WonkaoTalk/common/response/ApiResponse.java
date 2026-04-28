package com.example.WonkaoTalk.common.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonPropertyOrder({"status", "message", "data", "error", "timestamp"})
public class ApiResponse<T> {

  private final String status;
  private final String message;
  private final T data;
  private final String error;
  private final String timestamp;

  public static <T> ApiResponse<T> success(String message, T data) {
    return ApiResponse.<T>builder()
        .status("SUCCESS")
        .message(message)
        .data(data)
        .error(null)
        .timestamp(ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT))
        .build();
  }

  public static ApiResponse<Void> error(String code, String message) {
    return ApiResponse.<Void>builder()
        .status("ERROR")
        .message(message)
        .data(null)
        .error(code)
        .timestamp(ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT))
        .build();
  }
}
