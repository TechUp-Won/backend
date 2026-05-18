package com.example.WonkaoTalk.domain.payment.repo;

import com.example.WonkaoTalk.domain.payment.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepo extends JpaRepository<Payment, Long> {

  Optional<Payment> findByTossOrderId(String tossOrderId);
}
