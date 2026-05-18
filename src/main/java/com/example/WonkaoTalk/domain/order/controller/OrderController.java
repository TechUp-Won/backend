package com.example.WonkaoTalk.domain.order.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequest;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewRequest;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewResponse;
import com.example.WonkaoTalk.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  public ResponseEntity<ApiResponse<OrderCreateResponse>> createOrder(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody OrderCreateRequest requestDto
  ) {
    OrderCreateResponse responseDto = orderService.createOrder(userDetails.getUserId(), requestDto);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("주문이 생성되었습니다.", responseDto));
  }

  @PostMapping("/preview")
  public ResponseEntity<ApiResponse<OrderPreviewResponse>> previewOrder(
      @Valid @RequestBody OrderPreviewRequest requestDto
  ) {
    OrderPreviewResponse responseDto = orderService.previewOrder(requestDto);
    return ResponseEntity.ok(ApiResponse.success("주문 미리보기가 생성되었습니다.", responseDto));
  }
}
