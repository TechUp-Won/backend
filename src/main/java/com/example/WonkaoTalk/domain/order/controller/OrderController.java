package com.example.WonkaoTalk.domain.order.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewRequestDto;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewResponseDto;
import com.example.WonkaoTalk.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

//  @PostMapping
//  public ResponseEntity<ApiResponse<Void>> createOrder(
//      // Todo: 추후 진행
//      @RequestHeader("X-User-Id") Long userId,
//      @Valid @RequestBody OrderCreateRequestDto requestDto
//  ) {
//    orderService.createOrder(userId, requestDto);
//    return ResponseEntity.status(HttpStatus.CREATED)
//        .body(ApiResponse.success("주문이 생성되었습니다.", null));
//  }

  @PostMapping("/preview")
  public ResponseEntity<ApiResponse<OrderPreviewResponseDto>> previewOrder(
      @Valid @RequestBody OrderPreviewRequestDto requestDto
  ) {
    OrderPreviewResponseDto responseDto = orderService.previewOrder(requestDto);
    return ResponseEntity.ok(ApiResponse.success("주문 미리보기가 생성되었습니다.", responseDto));
  }
}
