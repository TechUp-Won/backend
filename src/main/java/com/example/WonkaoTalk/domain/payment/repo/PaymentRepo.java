package com.example.WonkaoTalk.domain.payment.repo;

import com.example.WonkaoTalk.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepo extends JpaRepository<Payment, Long> {

}
