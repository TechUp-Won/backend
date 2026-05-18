package com.example.WonkaoTalk.domain.payment.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.payment.client.TossPaymentConfirmResult;
import com.example.WonkaoTalk.domain.payment.client.TossPaymentsClient;
import com.example.WonkaoTalk.domain.payment.client.TossPaymentsException;
import com.example.WonkaoTalk.domain.payment.config.TossPaymentsProperties;
import com.example.WonkaoTalk.domain.payment.dto.PaymentCheckoutResponse;
import com.example.WonkaoTalk.domain.payment.dto.PaymentConfirmRequest;
import com.example.WonkaoTalk.domain.payment.dto.PaymentConfirmResponse;
import com.example.WonkaoTalk.domain.payment.dto.PaymentFailRequest;
import com.example.WonkaoTalk.domain.payment.dto.PaymentFailResponse;
import com.example.WonkaoTalk.domain.payment.entity.Payment;
import com.example.WonkaoTalk.domain.payment.entity.PaymentStatus;
import com.example.WonkaoTalk.domain.payment.repo.PaymentRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

  private final PaymentRepo paymentRepo;
  private final OrderItemRepo orderItemRepo;
  private final ProductVariantRepo productVariantRepo;
  private final TossPaymentsProperties tossPaymentsProperties;
  private final TossPaymentsClient tossPaymentsClient;

  // 주문 생성
  @Transactional
  public Payment createReadyPayment(Order order) {
    // 토스 주문 번호 만들기
    String tossOrderId = generateTossOrderId(order.getOrderNumber());

    // 멱등키 만들기
    String idempotencyKey = generateIdempotencyKey();

    LocalDateTime requestAt = LocalDateTime.now();

    Payment payment = Payment.createReadyPayment(
        order,
        tossOrderId,
        idempotencyKey,
        order.getFinalAmount(),
        requestAt
    );

    return paymentRepo.save(payment);

  }

  // 결제 정보 전달
  @Transactional
  public PaymentCheckoutResponse getCheckout(Long userId, Long paymentId) {
    // 토스페이 환경 설정 검증
    if (tossPaymentsProperties.getClientKey() == null
        || tossPaymentsProperties.getClientKey().isBlank()) {
      throw new BusinessException(ErrorCode.PAYMENT_CLIENT_KEY_NOT_CONFIGURED);
    }

    Payment payment = getPayment(paymentId);
    validateOwner(payment, userId);

    // 결제 상태인지 확인
    if (payment.getStatus() != PaymentStatus.READY
        && payment.getStatus() != PaymentStatus.PENDING) {
      throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS);
    }

    // 결제대기 상태 변경
    payment.markPending();

    return new PaymentCheckoutResponse(
        payment.getPaymentId(),
        tossPaymentsProperties.getClientKey(),
        payment.getTossOrderId(),
        payment.getTotalAmount(),
        payment.getOrder().getOrderTitle(),
        tossPaymentsProperties.getSuccessUrl(),
        tossPaymentsProperties.getFailUrl()
    );
  }

  // 결제 승인
  @Transactional
  public PaymentConfirmResponse confirm(Long userId, PaymentConfirmRequest request) {
    Payment payment = paymentRepo.findByTossOrderId(request.orderId())
        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    // 결제 최종 승인 전 재검증
    validateOwner(payment, userId);
    validateConfirmable(payment, request);

    try {
      // 토스 승인 전에 재고를 원자적으로 차감한다. 차감 실패 시 승인 요청을 보내지 않는다.
      decreaseStocks(payment.getOrder());

      // 토스 페이먼츠로 confirm api 요청
      TossPaymentConfirmResult result = tossPaymentsClient.confirm(
          request.paymentKey(),
          request.orderId(),
          payment.getTotalAmount(),
          payment.getIdempotencyKey()
      );
      validateTossConfirmResult(payment, result);

      payment.markPaid(result.paymentKey());
      payment.getOrder().markPaid();

      return new PaymentConfirmResponse(
          payment.getPaymentId(),
          payment.getOrder().getOrderId(),
          payment.getTossOrderId(),
          payment.getPaymentKey(),
          payment.getTotalAmount(),
          payment.getStatus().name(),
          result.method(),
          result.approvedAt(),
          result.receiptUrl()
      );
    } catch (TossPaymentsException e) {
      // 에러 로깅 처리
      log.warn(
          "TossPayments confirm failed. paymentId={}, tossOrderId={}, tossCode={}, tossMessage={}",
          payment.getPaymentId(), payment.getTossOrderId(), e.getCode(), e.getMessage());
      payment.markFailed(e.getCode(), e.getMessage());
      payment.getOrder().markPaymentFailed();
      throw new BusinessException(ErrorCode.PAYMENT_APPROVAL_FAILED);
    }
  }

  // 재고 감소 처리
  private void decreaseStocks(Order order) {
    List<OrderItem> orderItems = orderItemRepo.findByOrder(order);

    for (OrderItem orderItem : orderItems) {
      int updatedCount = productVariantRepo.decreaseStockAtomic(
          orderItem.getProductVariant().getId(),
          orderItem.getQuantity()
      );

      if (updatedCount != 1) {
        throw new BusinessException(ErrorCode.PROD_STOCK_INSUFFICIENT);
      }
    }
  }

  // 결제 최종 승인 전 취소 시
  // TODO: 추후.. Pending 상태인 결제를 일괄 정리하는 로직도 필요..
  @Transactional
  public PaymentFailResponse fail(Long userId, Long paymentId, PaymentFailRequest request) {
    Payment payment = findPaymentForFail(paymentId, request);
    validateOwner(payment, userId);

    if ("PAY_PROCESS_CANCELED".equals(request.code())) {
      payment.markCanceled(request.code(), request.message());
      payment.getOrder().markPaymentCanceled();
    } else {
      payment.markFailed(request.code(), request.message());
      payment.getOrder().markPaymentFailed();
    }

    return new PaymentFailResponse(
        payment.getPaymentId(),
        payment.getTossOrderId(),
        payment.getStatus().name(),
        payment.getFailCode(),
        payment.getFailMessage()
    );
  }

  // 결제 정보 가지고 오기
  private Payment getPayment(Long paymentId) {
    return paymentRepo.findById(paymentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
  }

  // 취소 처리해애햘 결제 정보 찾기
  private Payment findPaymentForFail(Long paymentId, PaymentFailRequest request) {
    if (request.orderId() == null || request.orderId().isBlank()) {
      return getPayment(paymentId);
    }
    Payment payment = paymentRepo.findByTossOrderId(request.orderId())
        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    if (!Objects.equals(payment.getPaymentId(), paymentId)) {
      throw new BusinessException(ErrorCode.PAYMENT_ORDER_MISMATCH);
    }
    return payment;
  }

  // 주문자 확인
  // TODO: Payment에서 Userid를 알기 위한 depth가 깊어서 추후 고민해야할 부분임.
  private void validateOwner(Payment payment, Long userId) {
    if (!Objects.equals(payment.getOrder().getUser().getId(), userId)) {
      throw new BusinessException(ErrorCode.PAYMENT_FORBIDDEN);
    }
  }

  // 승인 전 주문 금액, id, 상태값 검증
  private void validateConfirmable(Payment payment, PaymentConfirmRequest request) {
    if (payment.getStatus() != PaymentStatus.READY
        && payment.getStatus() != PaymentStatus.PENDING) {
      throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS);
    }
    if (!payment.getTossOrderId().equals(request.orderId())) {
      payment.markInvalid("ORDER_ID_MISMATCH", "결제 주문번호가 일치하지 않습니다.");
      payment.getOrder().markPaymentFailed();
      throw new BusinessException(ErrorCode.PAYMENT_ORDER_MISMATCH);
    }
    if (!payment.getTotalAmount().equals(request.amount())) {
      payment.markInvalid("AMOUNT_MISMATCH", "결제 금액이 일치하지 않습니다.");
      payment.getOrder().markPaymentFailed();
      throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }
  }

  // toss 측 승인 결과 확인
  private void validateTossConfirmResult(Payment payment, TossPaymentConfirmResult result) {
    if (result == null) {
      payment.markInvalid("EMPTY_TOSS_RESPONSE", "토스페이먼츠 승인 응답이 비어있습니다.");
      payment.getOrder().markPaymentFailed();
      throw new BusinessException(ErrorCode.PAYMENT_APPROVAL_FAILED);
    }
    if (!payment.getTossOrderId().equals(result.orderId())) {
      payment.markInvalid("TOSS_ORDER_ID_MISMATCH", "토스페이먼츠 승인 주문번호가 일치하지 않습니다.");
      payment.getOrder().markPaymentFailed();
      throw new BusinessException(ErrorCode.PAYMENT_ORDER_MISMATCH);
    }
    if (!payment.getTotalAmount().equals(result.totalAmount())) {
      payment.markInvalid("TOSS_AMOUNT_MISMATCH", "토스페이먼츠 승인 금액이 일치하지 않습니다.");
      payment.getOrder().markPaymentFailed();
      throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }
    if (!"DONE".equals(result.status())) {
      payment.markFailed("TOSS_STATUS_NOT_DONE", "토스페이먼츠 결제 상태가 DONE이 아닙니다.");
      throw new BusinessException(ErrorCode.PAYMENT_APPROVAL_FAILED);
    }
  }

  private String generateTossOrderId(String orderNumber) {
//    return orderNumber + "-PAY-" + UUID.randomUUID();
    String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    return orderNumber + "PAY" + uuid;
  }

  private String generateIdempotencyKey() {
    return UUID.randomUUID().toString();
  }
}
