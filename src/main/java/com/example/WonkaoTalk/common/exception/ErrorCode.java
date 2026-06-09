package com.example.WonkaoTalk.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
  // 공통
  BAD_REQUEST(400, "SYS-INVALID-INPUT", "입력값이 올바르지 않습니다."),
  UNAUTHORIZED(401, "SYS-EXPIRED-TOKEN", "토큰이 만료되었습니다."),
  FORBIDDEN(403, "SYS-FORBIDDEN-ACCESS", "권한이 없습니다."),
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
  AUTH_NOT_FOUND(404, "AUTH-NOT-FOUND", "회원 정보를 찾을 수 없습니다."),
  AUTH_SUSPECT_THEFT_TOKEN(401, "AUTH-SUSPECT-THEFT-TOKEN", "토큰 정보 이상이 감지되었습니다."),
  AUTH_MISSING_TOKEN(401, "AUTH-MISSING-TOKEN", "토큰 정보를 찾을 수 없습니다."),
  OAUTH_NULL_EMAIL(400, "OAUTH-NULL-EMAIL", "소셜 이메일 정보가 제공되지 않았습니다."),
  OAUTH_INVALID_PROVIDER(400, "OAUTH-INVALID-PROVIDER", "지원하지 않는 소셜 로그인입니다."),
  OAUTH_NULL_TOKEN(500, "OAUTH-NULL-TOKEN", "소셜 제공자 토큰 정보를 찾을 수 없습니다."),

  // 유저 도메인
  USER_NOT_FOUND(404, "USER-NOT-FOUND-ID", "해당 사용자 ID를 찾을 수 없습니다."),
  USER_SELF_REF(400, "USER-SELF-REF", "자기 자신을 참조 할 수 없습니다."),
  USER_REGISTERED_PHONE(409, "USER-REGISTERED-PHONE", "이미 등록 된 전화번호입니다."),
  // 친구 도메인
  FRND_SELF_REF(400, "FRND-SELF-REF", "자기 자신을 참조 할 수 없습니다."),
  FRND_REGISTERED_ALREADY(400, "FRND-REGISTERED-ALREADY", "이미 친구로 등록된 사용자입니다."),
  FRND_NOT_FOUND(404, "FRND-NOT-FOUND", "친구 정보를 찾을 수 없습니다."),

  // 판매자 도메인
  SELLER_NOT_FOUND(404, "SELLER-NOT-FOUND", "해당 판매자 ID를 찾을 수 없습니다."),
  SELLER_REGISTERED_ACCOUNT(400, "SELLER-REGISTERED-ACCOUNT", "이미 사업자 등록된 계정입니다."),
  SELLER_DUPLICATE_BUZNO(409, "SELLER-DUPLICATE-BUZNO", "이미 등록된 사업자 번호입니다."),

  // 스토어 도메인
  STORE_EXISTS_ALREADY(409, "STORE-EXISTS-ALREADY", "이미 등록된 스토어가 존재합니다."),
  STORE_NOT_FOUND(404, "STORE-NOT-FOUND", "스토어를 찾을 수 없습니다."),
  STORE_EXISTS_NAME(409, "STORE-EXISTS-NAME", "이미 존재하는 스토어 이름입니다"),
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
  PROD_MISMATCH_VARIANT_OPTION(400, "PROD-MISMATCH-VARIANT-OPTION",
      "variant의 옵션 이름이 등록된 옵션 목록과 일치하지 않습니다."),
  PROD_INVALID_STOCK_OPTION(400, "PROD-INVALID-STOCK-OPTION", "옵션이 있는 상품은 stock을 직접 입력할 수 없습니다."),
  PROD_DUPLICATE_SORT_ORDER(400, "PROD-DUPLICATE-SORT-ORDER", "sortOrder 값이 중복되었습니다."),
  PROD_INVALID_IMAGE_ID(400, "PROD-INVALID-IMAGE-ID", "해당 상품에 속하지 않는 이미지입니다."),
  PROD_HAS_ACTIVE_ORDER(409, "PROD-HAS-ACTIVE-ORDER", "진행 중인 주문이 있어 상품을 삭제할 수 없습니다."),
  PROD_VARIANT_NOT_FOUND(404, "PROD-NOT-FOUND-VARIANT", "해당 상품 옵션을 찾을 수 없습니다."),

  // 이미지 도메인
  IMAGE_INVALID_TYPE(400, "IMAGE-INVALID-TYPE", "허용되지 않는 파일 형식입니다. (jpg, jpeg, png, webp)"),
  IMAGE_INVALID_KEY(400, "IMAGE-INVALID-KEY", "유효하지 않은 이미지 키 형식입니다."),
  IMAGE_NOT_FOUND_KEY(400, "IMAGE-NOT-FOUND-KEY", "업로드되지 않았거나 만료된 이미지 키입니다."),

  // 배송지 도메인
  SHIP_NOT_FOUND(404, "SHIP-NOT-FOUND", "존재하지 않는 배송지입니다."),
  SHIP_CANNOT_DELETE_DEFAULT(409, "SHIP-CANNOT-DELETE-DEFAULT",
      "기본 배송지는 삭제할 수 없습니다. 먼저 다른 배송지를 기본으로 설정해 주세요."),

  // 주문 도메인
  ORDER_NOT_FOUND(404, "ORDER-NOT-FOUND", "주문을 찾을 수 없습니다."),
  ORDER_ITEM_NOT_FOUND(404, "ORDER-ITEM-NOT-FOUND", "주문 상품을 찾을 수 없습니다."),
  ORDER_INVALID_STATUS(409, "ORDER-INVALID-STATUS", "현재 주문 상태에서는 처리할 수 없습니다."),

  // 결제 도메인
  PAYMENT_NOT_FOUND(404, "PAYMENT-NOT-FOUND", "결제 정보를 찾을 수 없습니다."),
  PAYMENT_FORBIDDEN(403, "PAYMENT-FORBIDDEN", "해당 결제에 접근할 권한이 없습니다."),
  PAYMENT_INVALID_STATUS(409, "PAYMENT-INVALID-STATUS", "현재 결제 상태에서는 처리할 수 없습니다."),
  PAYMENT_AMOUNT_MISMATCH(400, "PAYMENT-AMOUNT-MISMATCH", "결제 금액이 일치하지 않습니다."),
  PAYMENT_ORDER_MISMATCH(400, "PAYMENT-ORDER-MISMATCH", "결제 주문번호가 일치하지 않습니다."),
  PAYMENT_APPROVAL_FAILED(502, "PAYMENT-APPROVAL-FAILED", "결제 승인에 실패했습니다."),
  PAYMENT_CLIENT_KEY_NOT_CONFIGURED(500, "PAYMENT-CLIENT-KEY-NOT-CONFIGURED",
      "토스페이먼츠 클라이언트 키가 설정되지 않았습니다."),
  PAYMENT_SECRET_KEY_NOT_CONFIGURED(500, "PAYMENT-SECRET-KEY-NOT-CONFIGURED",
      "토스페이먼츠 시크릿 키가 설정되지 않았습니다."),
  PAYMENT_ALREADY_PAID(409, "PAYMENT-ALREADY-PAID", "이미 결제가 완료된 주문입니다."),

  // 채팅 도메인
  ROOM_NOT_FOUND(404, "CHAT-NOT-FOUND-ROOM", "존재하지 않는 채팅방입니다."),
  CANNOT_CHAT_SELF(400, "CHAT-INVALID-SELF", "자기 자신과는 채팅방을 생성할 수 없습니다."),
  NOT_CHAT_PARTICIPANT(403, "CHAT-FORBIDDEN-PARTICIPANT", "해당 채팅방에 메시지를 보낼 권한이 없습니다."),
  MESSAGE_NOT_FOUND(404, "CHAT-NOT-FOUND-MESSAGE", "대상 메시지를 찾을 수 없습니다.");

  private final int httpStatus;
  private final String code;
  private final String message;
}
