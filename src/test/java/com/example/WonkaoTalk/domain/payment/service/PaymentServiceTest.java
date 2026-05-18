package com.example.WonkaoTalk.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.payment.client.TossPaymentConfirmResult;
import com.example.WonkaoTalk.domain.payment.client.TossPaymentsClient;
import com.example.WonkaoTalk.domain.payment.config.TossPaymentsProperties;
import com.example.WonkaoTalk.domain.payment.dto.PaymentCheckoutResponse;
import com.example.WonkaoTalk.domain.payment.dto.PaymentConfirmRequest;
import com.example.WonkaoTalk.domain.payment.dto.PaymentConfirmResponse;
import com.example.WonkaoTalk.domain.payment.entity.Payment;
import com.example.WonkaoTalk.domain.payment.entity.PaymentStatus;
import com.example.WonkaoTalk.domain.payment.entity.PgProvider;
import com.example.WonkaoTalk.domain.payment.repo.PaymentRepo;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

  @Mock
  private PaymentRepo paymentRepo;

  @Mock
  private OrderItemRepo orderItemRepo;

  @Mock
  private ProductVariantRepo productVariantRepo;

  @Mock
  private TossPaymentsProperties tossPaymentsProperties;

  @Mock
  private TossPaymentsClient tossPaymentsClient;

  @InjectMocks
  private PaymentService paymentService;

  @Test
  @DisplayName("결제 준비 Payment를 READY 상태로 생성한다.")
  public void createReadyPayment_CreatePaymentWithReadyStatus() {
    //given
    Order order = mock(Order.class);
    when(order.getOrderNumber()).thenReturn("ORD-1234");
    when(order.getFinalAmount()).thenReturn(35000L);
    when(paymentRepo.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

    //when
    Payment payment = paymentService.createReadyPayment(order);

    //then
    assertThat(payment.getOrder()).isEqualTo(order);
    assertThat(payment.getPgProvider()).isEqualTo(PgProvider.TOSS_PAYMENTS);
    assertThat(payment.getTossOrderId()).startsWith("ORD-1234PAY");
    assertThat(payment.getIdempotencyKey()).isNotBlank();
    assertThat(payment.getTotalAmount()).isEqualTo(35000L);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
  }

  @Test
  @DisplayName("결제창 호출 정보를 조회하면 Payment를 PENDING 상태로 변경한다.")
  public void getCheckout_MarkPaymentPendingAndReturnCheckoutInfo() {
    // given
    Order order = mockOrder(1L, "초콜릿 외 1건");
    Payment payment = Payment.createReadyPayment(order, "ORD-1234-PAY-abc", "idem-key", 35000L,
        LocalDateTime.now());
    when(paymentRepo.findById(10L)).thenReturn(Optional.of(payment));
    when(tossPaymentsProperties.getClientKey()).thenReturn("test_ck_123");
    when(tossPaymentsProperties.getSuccessUrl()).thenReturn("http://localhost:3000/success");
    when(tossPaymentsProperties.getFailUrl()).thenReturn("http://localhost:3000/fail");

    // when
    PaymentCheckoutResponse response = paymentService.getCheckout(1L, 10L);

    // then
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(response.clientKey()).isEqualTo("test_ck_123");
    assertThat(response.tossOrderId()).isEqualTo("ORD-1234-PAY-abc");
    assertThat(response.amount()).isEqualTo(35000L);
    assertThat(response.orderName()).isEqualTo("초콜릿 외 1건");
  }

  @Test
  @DisplayName("결제 승인 금액이 DB 금액과 다르면 토스 승인 요청 전에 INVALID 상태로 변경한다.")
  public void confirm_ThrowsExceptionWhenAmountMismatch() {
    // given
    Order order = mockOrder(1L, "초콜릿 외 1건");
    Payment payment = Payment.createReadyPayment(order, "ORD-1234-PAY-abc", "idem-key", 35000L,
        LocalDateTime.now());
    PaymentConfirmRequest request = new PaymentConfirmRequest("payment-key",
        "ORD-1234-PAY-abc", 30000L);
    when(paymentRepo.findByTossOrderId("ORD-1234-PAY-abc")).thenReturn(Optional.of(payment));

    // when & then
    assertThatThrownBy(() -> paymentService.confirm(1L, request))
        .isInstanceOf(BusinessException.class);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INVALID);
    verify(order).markPaymentFailed();
    verifyNoInteractions(tossPaymentsClient);
  }

  @Test
  @DisplayName("토스 결제 승인 성공 시 Payment와 Order를 결제 완료 상태로 변경한다.")
  public void confirm_MarkPaymentAndOrderPaidWhenTossConfirmSuccess() {
    // given
    Order order = mockOrder(1L, "초콜릿 외 1건");
    Payment payment = Payment.createReadyPayment(order, "ORD-1234-PAY-abc", "idem-key", 35000L,
        LocalDateTime.now());
    ProductVariant variant = mock(ProductVariant.class);
    OrderItem orderItem = mock(OrderItem.class);
    PaymentConfirmRequest request = new PaymentConfirmRequest("payment-key",
        "ORD-1234-PAY-abc", 35000L);
    TossPaymentConfirmResult result = new TossPaymentConfirmResult(
        "payment-key",
        "ORD-1234-PAY-abc",
        "DONE",
        "카드",
        35000L,
        "2026-05-16T12:00:00+09:00",
        new TossPaymentConfirmResult.Receipt("https://receipt.example")
    );
    when(paymentRepo.findByTossOrderId("ORD-1234-PAY-abc")).thenReturn(Optional.of(payment));
    when(orderItemRepo.findByOrder(order)).thenReturn(List.of(orderItem));
    when(orderItem.getProductVariant()).thenReturn(variant);
    when(orderItem.getQuantity()).thenReturn(2);
    when(variant.getId()).thenReturn(100L);
    when(productVariantRepo.decreaseStockAtomic(100L, 2)).thenReturn(1);
    when(tossPaymentsClient.confirm("payment-key", "ORD-1234-PAY-abc", 35000L, "idem-key"))
        .thenReturn(result);

    // when
    PaymentConfirmResponse response = paymentService.confirm(1L, request);

    // then
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(payment.getPaymentKey()).isEqualTo("payment-key");
    assertThat(response.status()).isEqualTo("PAID");
    assertThat(response.method()).isEqualTo("카드");
    assertThat(response.receiptUrl()).isEqualTo("https://receipt.example");
    verify(productVariantRepo).decreaseStockAtomic(100L, 2);
    verify(order).markPaid();
  }

  private Order mockOrder(Long userId, String orderTitle) {
    User user = mock(User.class);
    when(user.getId()).thenReturn(userId);

    Order order = mock(Order.class);
    when(order.getUser()).thenReturn(user);
    lenient().when(order.getOrderTitle()).thenReturn(orderTitle);
    lenient().when(order.getOrderId()).thenReturn(20L);
    return order;
  }
}
