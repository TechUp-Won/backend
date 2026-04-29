package com.example.WonkaoTalk.domain.product.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.product.dto.CartAddRequest;
import com.example.WonkaoTalk.domain.product.dto.CartAddResponse;
import com.example.WonkaoTalk.domain.product.dto.CartDeleteResponse;
import com.example.WonkaoTalk.domain.product.dto.CartOptionUpdateRequest;
import com.example.WonkaoTalk.domain.product.dto.CartOptionUpdateResponse;
import com.example.WonkaoTalk.domain.product.dto.CartQuantityUpdateRequest;
import com.example.WonkaoTalk.domain.product.dto.CartQuantityUpdateResponse;
import com.example.WonkaoTalk.domain.product.dto.CartResponse;
import com.example.WonkaoTalk.domain.product.service.CartService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

  private final CartService cartService;

  @GetMapping
  public ResponseEntity<ApiResponse<CartResponse>> getCart(
      // TODO: 사용자 인증 구현 시 Bearer Token에서 userId를 추출하도록 변경
      @RequestHeader("X-User-Id") Long userId) {
    CartResponse response = cartService.getCart(userId);
    return ResponseEntity.ok(ApiResponse.success("조회가 완료되었습니다", response));
  }

  @PostMapping("/items")
  public ResponseEntity<ApiResponse<CartAddResponse>> addToCart(
      // TODO: 사용자 인증 구현 시 Bearer Token에서 userId를 추출하도록 변경
      @RequestHeader("X-User-Id") Long userId,
      @RequestBody CartAddRequest request) {
    CartAddResponse response = cartService.addToCart(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("장바구니에 상품이 추가되었습니다.", response));
  }

  @PatchMapping("/items/{cartItemId}/quantity")
  public ResponseEntity<ApiResponse<CartQuantityUpdateResponse>> updateCartItemQuantity(
      // TODO: 사용자 인증 구현 시 Bearer Token에서 userId를 추출하도록 변경
      @RequestHeader("X-User-Id") Long userId,
      @PathVariable Long cartItemId,
      @RequestBody CartQuantityUpdateRequest request) {
    CartQuantityUpdateResponse response = cartService.updateCartItemQuantity(userId, cartItemId, request);
    return ResponseEntity.ok(ApiResponse.success("수정이 완료되었습니다", response));
  }

  @PatchMapping("/items/{cartItemId}/option")
  public ResponseEntity<ApiResponse<CartOptionUpdateResponse>> updateCartItemOption(
      // TODO: 사용자 인증 구현 시 Bearer Token에서 userId를 추출하도록 변경
      @RequestHeader("X-User-Id") Long userId,
      @PathVariable Long cartItemId,
      @RequestBody CartOptionUpdateRequest request) {
    CartOptionUpdateResponse response = cartService.updateCartItemOption(userId, cartItemId, request);
    return ResponseEntity.ok(ApiResponse.success("수정이 완료되었습니다", response));
  }

  @DeleteMapping("/items")
  public ResponseEntity<ApiResponse<CartDeleteResponse>> deleteFromCart(
      // TODO: 사용자 인증 구현 시 Bearer Token에서 userId를 추출하도록 변경
      @RequestHeader("X-User-Id") Long userId,
      @RequestParam(required = false) List<Long> cartItemIds,
      @RequestParam(defaultValue = "false") boolean isAllDelete) {
    CartDeleteResponse response = cartService.deleteFromCart(userId, cartItemIds, isAllDelete);
    String message = isAllDelete ? "장바구니를 비웠습니다." : "선택하신 상품이 장바구니에서 삭제되었습니다.";
    return ResponseEntity.ok(ApiResponse.success(message, response));
  }
}
