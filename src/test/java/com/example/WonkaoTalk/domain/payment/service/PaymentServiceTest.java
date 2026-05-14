package com.example.WonkaoTalk.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.payment.entity.Payment;
import com.example.WonkaoTalk.domain.payment.entity.PaymentStatus;
import com.example.WonkaoTalk.domain.payment.entity.PgProvider;
import com.example.WonkaoTalk.domain.payment.repo.PaymentRepo;
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
    assertThat(payment.getTossOrderId()).startsWith("ORD-1234-PAY-");
    assertThat(payment.getIdempotencyKey()).isNotBlank();
    assertThat(payment.getTotalAmount()).isEqualTo(35000L);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
  }
}
