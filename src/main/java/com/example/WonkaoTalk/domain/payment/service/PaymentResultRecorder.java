package com.example.WonkaoTalk.domain.payment.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.payment.entity.Payment;
import com.example.WonkaoTalk.domain.payment.event.PaymentFailedEvent;
import com.example.WonkaoTalk.domain.payment.event.PaymentInvalidEvent;
import com.example.WonkaoTalk.domain.payment.repo.PaymentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class PaymentResultRecorder {

  private final PaymentRepo paymentRepo;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFail(PaymentFailedEvent event) {
    Payment payment = paymentRepo.findById(event.paymentId())
        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

    payment.markFailed(event.failCode(), event.failMessage());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordInvalid(PaymentInvalidEvent event) {
    Payment payment = paymentRepo.findById(event.paymentId())
        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

    payment.markInvalid(event.failCode(), event.failMessage());
  }
}
