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

  // 인증 도메인
  AUTH_INVALID_EMAIL(400, "AUTH-INVALID-EMAIL", "올바른 이메일 형식이 아닙니다."),
  AUTH_INVALID_PASSWORD(400, "AUTH-INVALID-PASSWORD", "비밀번호는 영문 대/소문자와 숫자를 모두 포함하여 8자리 이상이어야 합니다."),
  AUTH_DUPLICATE_EMAIL(409, "AUTH-DUPLICATE-EMAIL", "이미 사용 중인 이메일입니다."),
  AUTH_MISMATCH_PASSWORD(400, "AUTH-MISMATCH-PASSWORD", "비밀번호가 일치하지 않습니다."),
  // UNAUTHORIZED와 동일함. INVALID_TOKEN과 구분을 위해 명시적 표현으로 이름 변경
  AUTH_EXPIRED_TOKEN(401, "AUTH-EXPIRED-TOKEN", "토큰이 만료되었습니다."),
  AUTH_INVALID_TOKEN(401, "AUTH-INVALID-TOKEN", "유효하지 않은 토큰입니다."),
  AUTH_LOGGED_OUT_TOKEN(401, "AUTH-LOGGED-OUT-TOKEN", "이미 로그아웃 된 토큰입니다."),

  // 유저 도메인
  USER_NOT_FOUND(404, "USER-NOT-FOUND-ID", "해당 사용자 ID를 찾을 수 없습니다."),

  // 판매자 도메인
  SELLER_NOT_FOUND(404, "SELLER-NOT-FOUND", "해당 판매자 ID를 찾을 수 없습니다."),
  SELLER_REGISTERED_ACCOUNT(400, "SELLER-REGISTERED-ACCOUNT", "이미 사업자 등록된 계정입니다."),
  SELLER_DUPLICATE_BUZNO(409, "SELLER-DUPLICATE-BUZNO", "이미 등록된 사업자 번호입니다."),
  // 상품 도메인
  PROD_INVALID_PRICE_RANGE(400, "PROD-INVALID-PRICE-RANGE", "최소 가격은 최대 가격보다 클 수 없습니다."),
  PROD_INVALID_SORT(400, "PROD-INVALID-SORT", "허용되지 않는 정렬 기준입니다."),
  PROD_INVALID_PAGE_SIZE(400, "PROD-INVALID-PAGE-SIZE", "조회 개수는 1 이상 100 이하여야 합니다."),
  PROD_INVALID_PRODUCT_ID(400, "PROD-INVALID-PRODUCT-ID", "상품 ID는 숫자여야 합니다."),
  PROD_CATEGORY_NOT_FOUND(404, "PROD-NOT-FOUND-CATEGORY", "존재하지 않는 카테고리입니다."),
  PROD_STORE_NOT_FOUND(404, "PROD-NOT-FOUND-STORE", "존재하지 않는 스토어입니다."),
  PROD_NOT_FOUND(404, "PROD-NOT-FOUND-PRODUCT", "해당 상품을 찾을 수 없습니다."),
  PROD_DELETED(410, "PROD-DELETED-PRODUCT", "삭제된 상품입니다."),
  PROD_CART_NOT_FOUND(404, "PROD-NOT-FOUND-CART", "존재하지 않는 장바구니입니다."),
  PROD_STOCK_INSUFFICIENT(400, "PROD-INSUFFICIENT-STOCK", "재고가 부족합니다."),
  PROD_VARIANT_UNAVAILABLE(400, "PROD-UNAVAILABLE-VARIANT", "구매 불가능한 상품 옵션입니다."),
  PROD_INVALID_QUANTITY(400, "PROD-INVALID-QUANTITY", "수량은 최소 1개 이상이어야 합니다."),
  PROD_INVALID_PRICE(400, "PROD-INVALID-PRICE", "가격은 0 이상이어야 합니다."),
  PROD_INVALID_DISCOUNT_RATE(400, "PROD-INVALID-DISCOUNT-RATE", "할인율은 0에서 100 사이여야 합니다."),
  PROD_MISMATCH_VARIANT_OPTION(400, "PROD-MISMATCH-VARIANT-OPTION", "variant의 옵션 이름이 등록된 옵션 목록과 일치하지 않습니다."),
  PROD_INVALID_STOCK_OPTION(400, "PROD-INVALID-STOCK-OPTION", "옵션이 있는 상품은 stock을 직접 입력할 수 없습니다."),
  PROD_DUPLICATE_SORT_ORDER(400, "PROD-DUPLICATE-SORT-ORDER", "sortOrder 값이 중복되었습니다."),

  // 이미지 도메인
  IMAGE_INVALID_TYPE(400, "IMAGE-INVALID-TYPE", "허용되지 않는 파일 형식입니다. (jpg, jpeg, png, webp)"),
  IMAGE_INVALID_KEY(400, "IMAGE-INVALID-KEY", "유효하지 않은 이미지 키 형식입니다."),
  IMAGE_NOT_FOUND_KEY(400, "IMAGE-NOT-FOUND-KEY", "업로드되지 않았거나 만료된 이미지 키입니다."),

  // 주문 도메인

  // 채팅 도메인
  ROOM_NOT_FOUND(404, "CHAT-NOT-FOUND-ROOM", "존재하지 않는 채팅방입니다."),
  CANNOT_CHAT_SELF(400, "CHAT-INVALID-SELF", "자기 자신과는 채팅방을 생성할 수 없습니다."),
  NOT_CHAT_PARTICIPANT(403, "CHAT-FORBIDDEN-PARTICIPANT", "해당 채팅방에 메시지를 보낼 권한이 없습니다."),
  MESSAGE_NOT_FOUND(404, "CHAT-NOT-FOUND-MESSAGE", "대상 메시지를 찾을 수 없습니다.");

  private final int httpStatus;
  private final String code;
  private final String message;
}
