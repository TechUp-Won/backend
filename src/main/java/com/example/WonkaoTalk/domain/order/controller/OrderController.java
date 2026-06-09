package com.example.WonkaoTalk.domain.order.controller;

import com.example.WonkaoTalk.common.config.OpenApiConfig;
import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequest;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderDetailResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderListResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewRequest;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewResponse;
import com.example.WonkaoTalk.domain.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "주문", description = "주문 생성과 주문 미리보기 API")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class OrderController {

  private final OrderService orderService;

  @Operation(summary = "주문 생성", description = "상품 옵션, 수량, 배송 정보를 기반으로 주문을 생성하고 결제 준비 정보를 반환합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<OrderCreateResponse>> createOrder(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody OrderCreateRequest requestDto
  ) {
    OrderCreateResponse responseDto = orderService.createOrder(userDetails.getUserId(), requestDto);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("주문이 생성되었습니다.", responseDto));
  }

  @Operation(summary = "주문 미리보기", description = "주문 생성 전 상품 금액, 할인 금액, 최종 결제 금액을 계산합니다.")
  @PostMapping("/preview")
  public ResponseEntity<ApiResponse<OrderPreviewResponse>> previewOrder(
      @Valid @RequestBody OrderPreviewRequest requestDto
  ) {
    OrderPreviewResponse responseDto = orderService.previewOrder(requestDto);
    return ResponseEntity.ok(ApiResponse.success("주문 미리보기가 생성되었습니다.", responseDto));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<OrderListResponse>> getOrderList(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
  ) {
    OrderListResponse orderListResponse = orderService.getOrders(userDetails.getUserId(), pageable);

    return ResponseEntity.ok(ApiResponse.success("주문 리스트 조회가 완료되었습니다.", orderListResponse));
  }

  @GetMapping("/{orderId}")
  public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long orderId
  ) {
    OrderDetailResponse orderDetailResponse = orderService.getOrderDetail(
        userDetails.getUserId(),
        orderId
    );

    return ResponseEntity.ok(ApiResponse.success("주문 상세 조회가 완료되었습니다.", orderDetailResponse));
  }

  @DeleteMapping("/{orderId}")
  public ResponseEntity<ApiResponse<Void>> deleteOrder(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long orderId
  ) {
    orderService.deleteOrder(userDetails.getUserId(), orderId);

    return ResponseEntity.ok(ApiResponse.success("주문 삭제가 완료되었습니다.", null));
  }
}
