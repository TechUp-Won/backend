package com.example.WonkaoTalk.domain.order.controller;

import com.example.WonkaoTalk.common.config.OpenApiConfig;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.payment.dto.PaymentCheckoutResponse;
import com.example.WonkaoTalk.domain.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders/{orderId}/payments")
@Tag(name = "주문", description = "주문 생성과 주문 미리보기 API")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class OrderPaymentController {

  private final PaymentService paymentService;


  @PostMapping("/checkout")
  @Operation(summary = "결제창 호출 정보 조회", description = "Payment 객체를 생성하고 토스 결제창 호출에 필요한 clientKey, orderId, 금액, 성공/실패 URL을 조회합니다.")
  public ResponseEntity<PaymentCheckoutResponse> paymentCheckout(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long orderId
  ) {
    return ResponseEntity.ok(paymentService.getCheckout(userDetails.getUserId(), orderId));
  }

}
