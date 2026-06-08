package com.example.WonkaoTalk.domain.payment.event;

public record PaymentInvalidEvent(Long paymentId, String failCode, String failMessage) {

}
