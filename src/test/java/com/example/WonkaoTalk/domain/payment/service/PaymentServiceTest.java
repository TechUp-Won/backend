package com.example.WonkaoTalk.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderStatus;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderRepo;
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
import com.example.WonkaoTalk.domain.payment.entity.PgProvider;
import com.example.WonkaoTalk.domain.payment.event.PaymentFailedEvent;
import com.example.WonkaoTalk.domain.payment.repo.PaymentRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

  @Mock
  private PaymentRepo paymentRepo;

  @Mock
  private OrderItemRepo orderItemRepo;

  @Mock
  private OrderRepo orderRepo;

  @Mock
  private ProductVariantRepo productVariantRepo;

  @Mock
  private TossPaymentsProperties tossPaymentsProperties;

  @Mock
  private TossPaymentsClient tossPaymentsClient;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private PaymentService paymentService;

  @Nested
  @DisplayName("checkout 검증")
  class CheckoutTest {

    @Test
    @DisplayName("결제창 정보를 반환하고 PENDING상태의 Payment를 생성한다.")
    public void success_getCheckout() {
      //given
      Long userId = 1L;
      Long orderId = 1L;

      Order order = mockOrder(userId, orderId, OrderStatus.PAYMENT_PENDING, "가나 초콜릿 외 1건", 35000L);
      when(orderRepo.findByUserIdAndOrderId(userId, orderId)).thenReturn(Optional.of(order));
      when(paymentRepo.existsByOrder_OrderIdAndStatus(orderId, PaymentStatus.PAID)).thenReturn(
          false);
      when(tossPaymentsProperties.getClientKey()).thenReturn("clientKey");
      when(tossPaymentsProperties.getSuccessUrl()).thenReturn("http://localhost:3000/success");
      when(tossPaymentsProperties.getFailUrl()).thenReturn("http://localhost:3000/fail");

      when(paymentRepo.save(any(Payment.class))).thenAnswer(
          invocation -> invocation.getArgument(0));

      //when
      PaymentCheckoutResponse response = paymentService.getCheckout(userId, orderId);

      //then
      ArgumentCaptor<Payment> paymentCapture = ArgumentCaptor.forClass(Payment.class);
      verify(paymentRepo).save(paymentCapture.capture());
      Payment payment = paymentCapture.getValue();

      assertThat(payment.getOrder()).isEqualTo(order);
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
      assertThat(payment.getPaymentKey()).isNull();
      assertThat(payment.getTotalAmount()).isEqualTo(35000L);
      assertThat(payment.getPgProvider()).isEqualTo(PgProvider.TOSS_PAYMENTS);

      assertThat(payment.getTossOrderId()).startsWith("ORD-1234PAY");
      assertThat(payment.getTossOrderId()).isNotBlank();
      assertThat(payment.getIdempotencyKey()).isNotBlank();
      assertThat(payment.getRequestedAt()).isNotNull();

      assertThat(response.paymentId()).isEqualTo(payment.getPaymentId());
      assertThat(response.clientKey()).isEqualTo("clientKey");
      assertThat(response.tossOrderId()).isEqualTo(payment.getTossOrderId());
      assertThat(response.amount()).isEqualTo(35000L);
      assertThat(response.orderName()).isEqualTo("가나 초콜릿 외 1건");
      assertThat(response.successUrl()).isEqualTo("http://localhost:3000/success");
      assertThat(response.failUrl()).isEqualTo("http://localhost:3000/fail");
    }

    @Test
    @DisplayName("주문이 존재하지 않으면 ORDER_NOT_FOUND 예외가 발생한다.")
    public void fail_getCheckout_orderNotFound() {
      //given
      Long userId = 1L;
      Long orderId = 1L;

      when(orderRepo.findByUserIdAndOrderId(userId, orderId)).thenReturn(Optional.empty());

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.getCheckout(userId, orderId));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
      verify(paymentRepo, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("주문 상태가 PAYMENT_PENDING이 아니면 ORDER_INVALID_STATUS 예외가 발생한다.")
    public void fail_getCheckout_invalidOrderStatus() {
      //given
      Long userId = 1L;
      Long orderId = 1L;

      Order order = mockOrder(userId, orderId, OrderStatus.CREATED, "가나 초콜릿 외 1건", 35000L);
      when(orderRepo.findByUserIdAndOrderId(userId, orderId)).thenReturn(Optional.of(order));

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.getCheckout(userId, orderId));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_INVALID_STATUS);
      verify(paymentRepo, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("이미 결제 완료된 주문이면 PAYMENT_ALREADY_PAID 예외가 발생한다.")
    public void fail_getCheckout_alreadyPaid() {
      //given
      Long userId = 1L;
      Long orderId = 1L;

      Order order = mockOrder(userId, orderId, OrderStatus.PAYMENT_PENDING, "가나 초콜릿 외 1건", 35000L);
      when(orderRepo.findByUserIdAndOrderId(userId, orderId)).thenReturn(Optional.of(order));
      when(paymentRepo.existsByOrder_OrderIdAndStatus(orderId, PaymentStatus.PAID)).thenReturn(
          true);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.getCheckout(userId, orderId));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ALREADY_PAID);
      verify(paymentRepo, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("토스페이먼츠 clientKey가 null이면 PAYMENT_CLIENT_KEY_NOT_CONFIGURED 예외가 발생한다.")
    public void fail_getCheckout_clientKeyIsNull() {
      //given
      Long userId = 1L;
      Long orderId = 1L;

      Order order = mockOrder(userId, orderId, OrderStatus.PAYMENT_PENDING, "가나 초콜릿 외 1건", 35000L);
      when(orderRepo.findByUserIdAndOrderId(userId, orderId)).thenReturn(Optional.of(order));
      when(paymentRepo.existsByOrder_OrderIdAndStatus(orderId, PaymentStatus.PAID)).thenReturn(
          false);
      when(tossPaymentsProperties.getClientKey()).thenReturn(null);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.getCheckout(userId, orderId));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_CLIENT_KEY_NOT_CONFIGURED);
      verify(paymentRepo, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("토스페이먼츠 clientKey가 blank이면 PAYMENT_CLIENT_KEY_NOT_CONFIGURED 예외가 발생한다.")
    public void fail_getCheckout_clientKeyIsBlank() {
      //given
      Long userId = 1L;
      Long orderId = 1L;

      Order order = mockOrder(userId, orderId, OrderStatus.PAYMENT_PENDING, "가나 초콜릿 외 1건", 35000L);
      when(orderRepo.findByUserIdAndOrderId(userId, orderId)).thenReturn(Optional.of(order));
      when(paymentRepo.existsByOrder_OrderIdAndStatus(orderId, PaymentStatus.PAID)).thenReturn(
          false);
      when(tossPaymentsProperties.getClientKey()).thenReturn(" ");

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.getCheckout(userId, orderId));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_CLIENT_KEY_NOT_CONFIGURED);
      verify(paymentRepo, never()).save(any(Payment.class));
    }

  }

  @Nested
  @DisplayName("confirm 검증")
  class ConfirmTest {

    @Test
    @DisplayName("토스 승인 성공 시 Payment와 Order를 결제 완료 상태로 변경하고 승인 응답을 반환한다.")
    public void success_confirm() {
      //given
      Long userId = 1L;
      Long orderId = 1L;
      Long paymentId = 10L;
      PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "ORD-1234PAY", 35000L);
      Payment payment = mockPendingPayment(userId, orderId, paymentId, request.orderId(), 35000L);
      TossPaymentConfirmResult result = mockTossConfirmResult("paymentKey", request.orderId(), "DONE",
          35000L);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));
      when(tossPaymentsClient.confirm(request.paymentKey(), request.orderId(), 35000L,
          payment.getIdempotencyKey())).thenReturn(result);

      //when
      PaymentConfirmResponse response = paymentService.confirm(userId, request);

      //then
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
      assertThat(payment.getPaymentKey()).isEqualTo("paymentKey");
      assertThat(payment.getApprovedAt()).isNotNull();
      verify(payment.getOrder()).markPaid();

      assertThat(response.paymentId()).isEqualTo(paymentId);
      assertThat(response.orderId()).isEqualTo(orderId);
      assertThat(response.tossOrderId()).isEqualTo(request.orderId());
      assertThat(response.paymentKey()).isEqualTo("paymentKey");
      assertThat(response.amount()).isEqualTo(35000L);
      assertThat(response.status()).isEqualTo("PAID");
      assertThat(response.method()).isEqualTo("카드");
      assertThat(response.approvedAt()).isEqualTo("2026-06-08T12:00:00+09:00");
      assertThat(response.receiptUrl()).isEqualTo("https://receipt.example");
    }

    @Test
    @DisplayName("결제 정보가 존재하지 않으면 PAYMENT_NOT_FOUND 예외가 발생한다.")
    public void fail_confirm_paymentNotFound() {
      //given
      Long userId = 1L;
      PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "ORD-1234PAY", 35000L);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.empty());

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.confirm(userId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
      verify(tossPaymentsClient, never()).confirm(any(), any(), any(), any());
    }

    @Test
    @DisplayName("결제 주문자가 아니면 PAYMENT_FORBIDDEN 예외가 발생한다.")
    public void fail_confirm_forbidden() {
      //given
      Long userId = 1L;
      PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "ORD-1234PAY", 35000L);
      Payment payment = mockPendingPayment(2L, 1L, request.orderId(), 35000L);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.confirm(userId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_FORBIDDEN);
      verify(tossPaymentsClient, never()).confirm(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Payment 상태가 PENDING이 아니면 PAYMENT_INVALID_STATUS 예외가 발생한다.")
    public void fail_confirm_invalidPaymentStatus() {
      //given
      Long userId = 1L;
      PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "ORD-1234PAY", 35000L);
      Payment payment = mockPendingPayment(userId, 1L, request.orderId(), 35000L);
      payment.markPaid("alreadyPaidPaymentKey");

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.confirm(userId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_STATUS);
      verify(tossPaymentsClient, never()).confirm(any(), any(), any(), any());
    }

    @Test
    @DisplayName("주문 상태가 PAYMENT_PENDING이 아니면 ORDER_INVALID_STATUS 예외가 발생한다.")
    public void fail_confirm_invalidOrderStatus() {
      //given
      Long userId = 1L;
      PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "ORD-1234PAY", 35000L);
      Payment payment = mockPendingPayment(userId, 1L, null, request.orderId(), 35000L,
          OrderStatus.CREATED);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.confirm(userId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_INVALID_STATUS);
      verify(tossPaymentsClient, never()).confirm(any(), any(), any(), any());
    }

    @Test
    @DisplayName("결제 주문번호가 일치하지 않으면 PAYMENT_ORDER_MISMATCH 예외가 발생하고 INVALID 상태로 변경된다.")
    public void fail_confirm_orderMismatch() {
      //given
      Long userId = 1L;
      PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "ORD-1234PAY", 35000L);
      Payment payment = mockPendingPayment(userId, 1L, "ORD-5678PAY", 35000L);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.confirm(userId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ORDER_MISMATCH);
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INVALID);
      assertThat(payment.getFailCode()).isEqualTo("ORDER_ID_MISMATCH");
      verify(tossPaymentsClient, never()).confirm(any(), any(), any(), any());
    }

    @Test
    @DisplayName("결제 금액이 일치하지 않으면 PAYMENT_AMOUNT_MISMATCH 예외가 발생하고 INVALID 상태로 변경된다.")
    public void fail_confirm_amountMismatch() {
      //given
      Long userId = 1L;
      PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "ORD-1234PAY", 30000L);
      Payment payment = mockPendingPayment(userId, 1L, request.orderId(), 35000L);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.confirm(userId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INVALID);
      assertThat(payment.getFailCode()).isEqualTo("AMOUNT_MISMATCH");
      verify(tossPaymentsClient, never()).confirm(any(), any(), any(), any());
    }

    @Test
    @DisplayName("토스 승인 응답이 null이면 PAYMENT_APPROVAL_FAILED 예외가 발생하고 INVALID 상태로 변경된다.")
    public void fail_confirm_emptyTossResponse() {
      //given
      Long userId = 1L;
      PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "ORD-1234PAY", 35000L);
      Payment payment = mockPendingPayment(userId, 1L, request.orderId(), 35000L);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));
      when(tossPaymentsClient.confirm(request.paymentKey(), request.orderId(), 35000L,
          payment.getIdempotencyKey())).thenReturn(null);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.confirm(userId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_APPROVAL_FAILED);
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INVALID);
      assertThat(payment.getFailCode()).isEqualTo("EMPTY_TOSS_RESPONSE");
    }

    @Test
    @DisplayName("토스 승인 주문번호가 일치하지 않으면 PAYMENT_ORDER_MISMATCH 예외가 발생하고 INVALID 상태로 변경된다.")
    public void fail_confirm_tossOrderMismatch() {
      //given
      Long userId = 1L;
      PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "ORD-1234PAY", 35000L);
      Payment payment = mockPendingPayment(userId, 1L, request.orderId(), 35000L);
      TossPaymentConfirmResult result = mockTossConfirmResult("paymentKey", "ORD-5678PAY", "DONE",
          35000L);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));
      when(tossPaymentsClient.confirm(request.paymentKey(), request.orderId(), 35000L,
          payment.getIdempotencyKey())).thenReturn(result);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.confirm(userId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ORDER_MISMATCH);
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INVALID);
      assertThat(payment.getFailCode()).isEqualTo("TOSS_ORDER_ID_MISMATCH");
    }

    @Test
    @DisplayName("토스 승인 금액이 일치하지 않으면 PAYMENT_AMOUNT_MISMATCH 예외가 발생하고 INVALID 상태로 변경된다.")
    public void fail_confirm_tossAmountMismatch() {
      //given
      Long userId = 1L;
      PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "ORD-1234PAY", 35000L);
      Payment payment = mockPendingPayment(userId, 1L, request.orderId(), 35000L);
      TossPaymentConfirmResult result = mockTossConfirmResult("paymentKey", request.orderId(), "DONE",
          30000L);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));
      when(tossPaymentsClient.confirm(request.paymentKey(), request.orderId(), 35000L,
          payment.getIdempotencyKey())).thenReturn(result);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.confirm(userId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INVALID);
      assertThat(payment.getFailCode()).isEqualTo("TOSS_AMOUNT_MISMATCH");
    }

    @Test
    @DisplayName("토스 승인 상태가 DONE이 아니면 PAYMENT_APPROVAL_FAILED 예외가 발생하고 FAILED 상태로 변경된다.")
    public void fail_confirm_tossStatusNotDone() {
      //given
      Long userId = 1L;
      PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "ORD-1234PAY", 35000L);
      Payment payment = mockPendingPayment(userId, 1L, request.orderId(), 35000L);
      TossPaymentConfirmResult result = mockTossConfirmResult("paymentKey", request.orderId(), "WAITING",
          35000L);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));
      when(tossPaymentsClient.confirm(request.paymentKey(), request.orderId(), 35000L,
          payment.getIdempotencyKey())).thenReturn(result);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.confirm(userId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_APPROVAL_FAILED);
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
      assertThat(payment.getFailCode()).isEqualTo("TOSS_STATUS_NOT_DONE");
    }

    @Test
    @DisplayName("토스 승인 요청이 실패하면 PAYMENT_APPROVAL_FAILED 예외가 발생하고 결제 실패 이벤트를 발행한다.")
    public void fail_confirm_tossPaymentsException() {
      //given
      Long userId = 1L;
      PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "ORD-1234PAY", 35000L);
      Payment payment = mockPendingPayment(userId, 1L, request.orderId(), 35000L);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));
      when(tossPaymentsClient.confirm(request.paymentKey(), request.orderId(), 35000L,
          payment.getIdempotencyKey())).thenThrow(
          new TossPaymentsException("TOSS_ERROR", "토스 승인 실패"));

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.confirm(userId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_APPROVAL_FAILED);
      verify(eventPublisher).publishEvent(any(PaymentFailedEvent.class));
    }
  }

  @Nested
  @DisplayName("fail 검증")
  class FailTest {

    @Test
    @DisplayName("결제 취소 코드이면 Payment를 ABORTED 상태로 변경하고 실패 응답을 반환한다.")
    public void success_fail_payProcessCanceled() {
      //given
      Long userId = 1L;
      Long orderId = 1L;
      Long paymentId = 10L;
      PaymentFailRequest request = new PaymentFailRequest(
          "ORD-1234PAY",
          "PAY_PROCESS_CANCELED",
          "사용자가 결제를 취소했습니다."
      );
      Payment payment = mockPendingPayment(userId, orderId, paymentId, request.orderId(), 35000L);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));

      //when
      PaymentFailResponse response = paymentService.fail(userId, paymentId, request);

      //then
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.ABORTED);
      assertThat(payment.getFailCode()).isEqualTo("PAY_PROCESS_CANCELED");
      assertThat(payment.getFailMessage()).isEqualTo("사용자가 결제를 취소했습니다.");
      assertThat(payment.getFailedAt()).isNotNull();

      assertThat(response.paymentId()).isEqualTo(paymentId);
      assertThat(response.tossOrderId()).isEqualTo(request.orderId());
      assertThat(response.status()).isEqualTo("ABORTED");
      assertThat(response.failCode()).isEqualTo("PAY_PROCESS_CANCELED");
      assertThat(response.failMessage()).isEqualTo("사용자가 결제를 취소했습니다.");
    }

    @Test
    @DisplayName("결제 실패 코드이면 Payment를 FAILED 상태로 변경하고 실패 응답을 반환한다.")
    public void success_fail_paymentFailed() {
      //given
      Long userId = 1L;
      Long orderId = 1L;
      Long paymentId = 10L;
      PaymentFailRequest request = new PaymentFailRequest(
          "ORD-1234PAY",
          "REJECT_CARD_COMPANY",
          "카드사에서 결제를 거절했습니다."
      );
      Payment payment = mockPendingPayment(userId, orderId, paymentId, request.orderId(), 35000L);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));

      //when
      PaymentFailResponse response = paymentService.fail(userId, paymentId, request);

      //then
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
      assertThat(payment.getFailCode()).isEqualTo("REJECT_CARD_COMPANY");
      assertThat(payment.getFailMessage()).isEqualTo("카드사에서 결제를 거절했습니다.");
      assertThat(payment.getFailedAt()).isNotNull();

      assertThat(response.paymentId()).isEqualTo(paymentId);
      assertThat(response.tossOrderId()).isEqualTo(request.orderId());
      assertThat(response.status()).isEqualTo("FAILED");
      assertThat(response.failCode()).isEqualTo("REJECT_CARD_COMPANY");
      assertThat(response.failMessage()).isEqualTo("카드사에서 결제를 거절했습니다.");
    }

    @Test
    @DisplayName("요청 orderId가 blank이면 paymentId로 Payment를 찾아 실패 처리한다.")
    public void success_fail_findPaymentByPaymentIdWhenOrderIdIsBlank() {
      //given
      Long userId = 1L;
      Long orderId = 1L;
      Long paymentId = 10L;
      PaymentFailRequest request = new PaymentFailRequest(
          " ",
          "REJECT_CARD_COMPANY",
          "카드사에서 결제를 거절했습니다."
      );
      Payment payment = mockPendingPayment(userId, orderId, paymentId, "ORD-1234PAY", 35000L);

      when(paymentRepo.findById(paymentId)).thenReturn(Optional.of(payment));

      //when
      PaymentFailResponse response = paymentService.fail(userId, paymentId, request);

      //then
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
      assertThat(response.paymentId()).isEqualTo(paymentId);
      assertThat(response.tossOrderId()).isEqualTo("ORD-1234PAY");
      assertThat(response.status()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Payment가 존재하지 않으면 PAYMENT_NOT_FOUND 예외가 발생한다.")
    public void fail_fail_paymentNotFound() {
      //given
      Long userId = 1L;
      Long paymentId = 10L;
      PaymentFailRequest request = new PaymentFailRequest("ORD-1234PAY", "FAIL_CODE", "결제 실패");

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.empty());

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.fail(userId, paymentId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("요청 paymentId와 Payment의 id가 다르면 PAYMENT_ORDER_MISMATCH 예외가 발생한다.")
    public void fail_fail_paymentOrderMismatch() {
      //given
      Long userId = 1L;
      Long orderId = 1L;
      Long paymentId = 10L;
      PaymentFailRequest request = new PaymentFailRequest("ORD-1234PAY", "FAIL_CODE", "결제 실패");
      Payment payment = mockPendingPayment(userId, orderId, 20L, request.orderId(), 35000L);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.fail(userId, paymentId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ORDER_MISMATCH);
    }

    @Test
    @DisplayName("결제 주문자가 아니면 PAYMENT_FORBIDDEN 예외가 발생한다.")
    public void fail_fail_forbidden() {
      //given
      Long userId = 1L;
      Long orderId = 1L;
      Long paymentId = 10L;
      PaymentFailRequest request = new PaymentFailRequest("ORD-1234PAY", "FAIL_CODE", "결제 실패");
      Payment payment = mockPendingPayment(2L, orderId, paymentId, request.orderId(), 35000L);

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.fail(userId, paymentId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_FORBIDDEN);
    }

    @Test
    @DisplayName("Payment 상태가 PENDING이 아니면 PAYMENT_INVALID_STATUS 예외가 발생한다.")
    public void fail_fail_invalidPaymentStatus() {
      //given
      Long userId = 1L;
      Long orderId = 1L;
      Long paymentId = 10L;
      PaymentFailRequest request = new PaymentFailRequest("ORD-1234PAY", "FAIL_CODE", "결제 실패");
      Payment payment = mockPendingPayment(userId, orderId, paymentId, request.orderId(), 35000L);
      payment.markPaid("paymentKey");

      when(paymentRepo.findByTossOrderId(request.orderId())).thenReturn(Optional.of(payment));

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentService.fail(userId, paymentId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_STATUS);
    }
  }

  private Order mockOrder(
      Long userId,
      Long orderId,
      OrderStatus status,
      String orderTitle,
      Long finalAmount
  ) {
    User user = mock(User.class);
    lenient().when(user.getId()).thenReturn(userId);

    Order order = mock(Order.class);
    lenient().when(order.getUser()).thenReturn(user);
    lenient().when(order.getOrderId()).thenReturn(orderId);
    lenient().when(order.getOrderStatus()).thenReturn(status);
    lenient().when(order.getOrderTitle()).thenReturn(orderTitle);
    lenient().when(order.getFinalAmount()).thenReturn(finalAmount);
    lenient().when(order.getOrderNumber()).thenReturn("ORD-1234");

    return order;
  }

  private Payment mockPendingPayment(
      Long userId,
      Long orderId,
      String tossOrderId,
      Long totalAmount
  ) {
    return mockPendingPayment(userId, orderId, null, tossOrderId, totalAmount);
  }

  private Payment mockPendingPayment(
      Long userId,
      Long orderId,
      Long paymentId,
      String tossOrderId,
      Long totalAmount
  ) {
    return mockPendingPayment(userId, orderId, paymentId, tossOrderId, totalAmount,
        OrderStatus.PAYMENT_PENDING);
  }

  private Payment mockPendingPayment(
      Long userId,
      Long orderId,
      Long paymentId,
      String tossOrderId,
      Long totalAmount,
      OrderStatus orderStatus
  ) {
    Order order = mockOrder(userId, orderId, orderStatus, "가나 초콜릿 외 1건",
        totalAmount);
    Payment payment = Payment.createPendingPayment(order, tossOrderId, "idempotencyKey", totalAmount,
        LocalDateTime.now());
    ReflectionTestUtils.setField(payment, "paymentId", paymentId);
    return payment;
  }

  private TossPaymentConfirmResult mockTossConfirmResult(
      String paymentKey,
      String orderId,
      String status,
      Long totalAmount
  ) {
    return new TossPaymentConfirmResult(
        paymentKey,
        orderId,
        status,
        "카드",
        totalAmount,
        "2026-06-08T12:00:00+09:00",
        new TossPaymentConfirmResult.Receipt("https://receipt.example")
    );
  }
}
