package com.example.WonkaoTalk.domain.product.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.product.dto.CartAddRequest;
import com.example.WonkaoTalk.domain.product.dto.CartAddResponse;
import com.example.WonkaoTalk.domain.product.dto.CartDeleteResponse;
import com.example.WonkaoTalk.domain.product.dto.CartOptionUpdateRequest;
import com.example.WonkaoTalk.domain.product.dto.CartOptionUpdateResponse;
import com.example.WonkaoTalk.domain.product.dto.CartQuantityUpdateRequest;
import com.example.WonkaoTalk.domain.product.dto.CartQuantityUpdateResponse;
import com.example.WonkaoTalk.domain.product.dto.CartResponse;
import com.example.WonkaoTalk.domain.product.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
@Tag(name = "장바구니", description = "장바구니 조회, 상품 추가, 수량/옵션 변경, 상품 삭제 API")
public class CartController {

  private final CartService cartService;

  @Operation(summary = "장바구니 조회", description = "로그인한 사용자의 장바구니 상품 목록과 금액 요약을 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<CartResponse>> getCart(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    CartResponse response = cartService.getCart(userDetails.getAuthId());
    return ResponseEntity.ok(ApiResponse.success("조회가 완료되었습니다", response));
  }

  @Operation(summary = "장바구니 상품 추가", description = "선택한 상품 옵션과 수량을 장바구니에 추가합니다.")
  @PostMapping("/items")
  public ResponseEntity<ApiResponse<CartAddResponse>> addToCart(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody CartAddRequest request) {
    CartAddResponse response = cartService.addToCart(userDetails.getAuthId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("장바구니에 상품이 추가되었습니다.", response));
  }

  @Operation(summary = "장바구니 상품 수량 변경", description = "장바구니 상품의 구매 수량을 변경합니다.")
  @PatchMapping("/items/{cartItemId}/quantity")
  public ResponseEntity<ApiResponse<CartQuantityUpdateResponse>> updateCartItemQuantity(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long cartItemId,
      @Valid @RequestBody CartQuantityUpdateRequest request) {
    CartQuantityUpdateResponse response = cartService.updateCartItemQuantity(
        userDetails.getAuthId(), cartItemId, request);
    return ResponseEntity.ok(ApiResponse.success("수정이 완료되었습니다", response));
  }

  @Operation(summary = "장바구니 상품 옵션 변경", description = "장바구니 상품의 선택 옵션을 다른 옵션으로 변경합니다.")
  @PatchMapping("/items/{cartItemId}/option")
  public ResponseEntity<ApiResponse<CartOptionUpdateResponse>> updateCartItemOption(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long cartItemId,
      @Valid @RequestBody CartOptionUpdateRequest request) {
    CartOptionUpdateResponse response = cartService.updateCartItemOption(
        userDetails.getAuthId(), cartItemId, request);
    return ResponseEntity.ok(ApiResponse.success("수정이 완료되었습니다", response));
  }

  @Operation(summary = "장바구니 상품 삭제", description = "선택한 장바구니 상품을 삭제하거나 장바구니 전체를 비웁니다.")
  @DeleteMapping("/items")
  public ResponseEntity<ApiResponse<CartDeleteResponse>> deleteFromCart(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(required = false) List<Long> cartItemIds,
      @RequestParam(defaultValue = "false") boolean isAllDelete) {
    CartDeleteResponse response = cartService.deleteFromCart(
        userDetails.getAuthId(), cartItemIds, isAllDelete);
    String message = isAllDelete ? "장바구니를 비웠습니다." : "선택하신 상품이 장바구니에서 삭제되었습니다.";
    return ResponseEntity.ok(ApiResponse.success(message, response));
  }
}
