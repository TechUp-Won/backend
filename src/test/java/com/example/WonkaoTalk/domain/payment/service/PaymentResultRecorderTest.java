package com.example.WonkaoTalk.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.payment.entity.Payment;
import com.example.WonkaoTalk.domain.payment.entity.PaymentStatus;
import com.example.WonkaoTalk.domain.payment.event.PaymentFailedEvent;
import com.example.WonkaoTalk.domain.payment.event.PaymentInvalidEvent;
import com.example.WonkaoTalk.domain.payment.repo.PaymentRepo;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PaymentResultRecorderTest {

  @Mock
  private PaymentRepo paymentRepo;

  @InjectMocks
  private PaymentResultRecorder paymentResultRecorder;

  @Nested
  @DisplayName("결제 실패 기록 검증")
  class RecordFailTest {

    @Test
    @DisplayName("결제 실패 이벤트를 받으면 Payment를 FAILED 상태로 변경하고 실패 사유를 기록한다.")
    public void success_recordFail() {
      //given
      Long paymentId = 1L;
      PaymentFailedEvent event = new PaymentFailedEvent(paymentId, "TOSS_ERROR", "토스 승인 실패");
      Payment payment = mockPendingPayment();

      when(paymentRepo.findById(paymentId)).thenReturn(Optional.of(payment));

      //when
      paymentResultRecorder.recordFail(event);

      //then
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
      assertThat(payment.getFailCode()).isEqualTo("TOSS_ERROR");
      assertThat(payment.getFailMessage()).isEqualTo("토스 승인 실패");
      assertThat(payment.getFailedAt()).isNotNull();
    }

    @Test
    @DisplayName("Payment가 존재하지 않으면 PAYMENT_NOT_FOUND 예외가 발생한다.")
    public void fail_recordFail_paymentNotFound() {
      //given
      Long paymentId = 1L;
      PaymentFailedEvent event = new PaymentFailedEvent(paymentId, "TOSS_ERROR", "토스 승인 실패");

      when(paymentRepo.findById(paymentId)).thenReturn(Optional.empty());

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentResultRecorder.recordFail(event));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("결제 유효성 실패 기록 검증")
  class RecordInvalidTest {

    @Test
    @DisplayName("결제 유효성 실패 이벤트를 받으면 Payment를 INVALID 상태로 변경하고 실패 사유를 기록한다.")
    public void success_recordInvalid() {
      //given
      Long paymentId = 1L;
      PaymentInvalidEvent event = new PaymentInvalidEvent(paymentId, "AMOUNT_MISMATCH",
          "결제 금액이 일치하지 않습니다.");
      Payment payment = mockPendingPayment();

      when(paymentRepo.findById(paymentId)).thenReturn(Optional.of(payment));

      //when
      paymentResultRecorder.recordInvalid(event);

      //then
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INVALID);
      assertThat(payment.getFailCode()).isEqualTo("AMOUNT_MISMATCH");
      assertThat(payment.getFailMessage()).isEqualTo("결제 금액이 일치하지 않습니다.");
      assertThat(payment.getFailedAt()).isNotNull();
    }

    @Test
    @DisplayName("Payment가 존재하지 않으면 PAYMENT_NOT_FOUND 예외가 발생한다.")
    public void fail_recordInvalid_paymentNotFound() {
      //given
      Long paymentId = 1L;
      PaymentInvalidEvent event = new PaymentInvalidEvent(paymentId, "AMOUNT_MISMATCH",
          "결제 금액이 일치하지 않습니다.");

      when(paymentRepo.findById(paymentId)).thenReturn(Optional.empty());

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> paymentResultRecorder.recordInvalid(event));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }
  }

  private Payment mockPendingPayment() {
    Order order = mock(Order.class);

    return Payment.createPendingPayment(
        order,
        "ORD-1234PAY",
        "idempotencyKey",
        35000L,
        LocalDateTime.now()
    );
  }
}
