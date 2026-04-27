package com.example.WonkaoTalk.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
  // 공통
  BAD_REQUEST(400, "SYS-INVALID-INPUT", "입력값이 올바르지 않습니다."),
  UNAUTHORIZED(401, "AUTH-EXPIRED-TOKEN", "토큰이 만료되었습니다."),
  FORBIDDEN(403, "AUTH-FORBIDDEN-ACCESS", "권한이 없습니다."),
  NOT_FOUND(404, "SYS-NOT-FOUND", "데이터가 없습니다."),
  SERVER_ERROR(500, "SYS-INTERNAL-ERROR", "서버 내부 에러가 발생했습니다."),
  SERVICE_UNAVAILABLE(503, "SYS-SERVICE-UNAVAILABLE", "서버 점검 중입니다."),

  // 유저 도메인
  USER_NOT_FOUND(404, "USER-NOTFOUND-ID", "해당 사용자 ID를 찾을 수 없습니다."),

  // 상품 도메인

  // 주문 도메인

  // 채팅 도메인
  ROOM_NOT_FOUND(404, "CHAT-NOTFOUND-ROOM", "존재하지 않는 채팅방입니다.");

  private final int httpStatus;
  private final String code;
  private final String message;
}
