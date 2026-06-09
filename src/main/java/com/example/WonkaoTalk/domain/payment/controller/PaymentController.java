package com.example.WonkaoTalk.domain.payment.controller;

import com.example.WonkaoTalk.common.config.OpenApiConfig;
import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.payment.dto.PaymentConfirmRequest;
import com.example.WonkaoTalk.domain.payment.dto.PaymentConfirmResponse;
import com.example.WonkaoTalk.domain.payment.dto.PaymentFailRequest;
import com.example.WonkaoTalk.domain.payment.dto.PaymentFailResponse;
import com.example.WonkaoTalk.domain.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "결제", description = "토스페이먼츠 결제창 호출, 결제 승인, 결제 실패/취소 처리 API")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class PaymentController {

  private final PaymentService paymentService;
  
  @Operation(summary = "결제 승인", description = "토스 결제 인증 후 전달받은 paymentKey, orderId, amount를 검증하고 최종 승인합니다.")
  @PostMapping("/confirm")
  public ResponseEntity<ApiResponse<PaymentConfirmResponse>> confirm(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody PaymentConfirmRequest request
  ) {
    PaymentConfirmResponse response = paymentService.confirm(userDetails.getUserId(), request);
    return ResponseEntity.ok(ApiResponse.success("결제가 승인되었습니다.", response));
  }

  @Operation(summary = "결제 실패/취소 저장", description = "토스 결제창 실패 또는 사용자 취소 정보를 주문과 결제 상태에 반영합니다.")
  @PostMapping("/{paymentId}/fail")
  public ResponseEntity<ApiResponse<PaymentFailResponse>> fail(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long paymentId,
      @Valid @RequestBody PaymentFailRequest request
  ) {
    PaymentFailResponse response = paymentService.fail(userDetails.getUserId(), paymentId,
        request);
    return ResponseEntity.ok(ApiResponse.success("결제 실패 정보가 저장되었습니다.", response));
  }
}
