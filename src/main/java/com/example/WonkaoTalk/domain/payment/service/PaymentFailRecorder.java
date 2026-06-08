package com.example.WonkaoTalk.domain.payment.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.payment.entity.Payment;
import com.example.WonkaoTalk.domain.payment.event.PaymentFailedEvent;
import com.example.WonkaoTalk.domain.payment.repo.PaymentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class PaymentFailRecorder {

  private final PaymentRepo paymentRepo;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFail(PaymentFailedEvent event) {
    Payment payment = paymentRepo.findById(event.paymentId())
        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

    payment.markFailed(event.failCode(), event.failMessage());
  }
}
