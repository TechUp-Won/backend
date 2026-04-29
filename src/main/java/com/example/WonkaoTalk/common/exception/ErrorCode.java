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

  // 주문 도메인

  // 채팅 도메인
  ROOM_NOT_FOUND(404, "CHAT-NOTFOUND-ROOM", "존재하지 않는 채팅방입니다."),
  CANNOT_CHAT_SELF(400, "CHAT-INVALID-SELF", "자기 자신과는 채팅방을 생성할 수 없습니다.");

  private final int httpStatus;
  private final String code;
  private final String message;
}
