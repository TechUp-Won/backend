package com.example.WonkaoTalk.domain.payment.event;

public record PaymentFailedEvent(Long paymentId, String failCode, String failMessage) {

}
