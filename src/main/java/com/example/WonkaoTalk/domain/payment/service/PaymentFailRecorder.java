package com.example.WonkaoTalk.domain.payment.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.payment.entity.Payment;
import com.example.WonkaoTalk.domain.payment.repo.PaymentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentFailRecorder {

  private final PaymentRepo paymentRepo;

  // 결제 실패 시 실패 기록 표시용
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFail(Long paymentId, String failCode, String failMessage) {
    Payment payment = paymentRepo.findById(paymentId).orElseThrow(() -> new BusinessException(
        ErrorCode.PAYMENT_NOT_FOUND));

    payment.markFailed(failCode, failMessage);
    payment.getOrder().markPaymentFailed();
  }
}
