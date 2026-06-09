package com.example.WonkaoTalk.domain.payment.repo;

import com.example.WonkaoTalk.domain.payment.entity.Payment;
import com.example.WonkaoTalk.domain.payment.entity.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepo extends JpaRepository<Payment, Long> {

  Optional<Payment> findByTossOrderId(String tossOrderId);

  List<Payment> findByOrder_OrderIdOrderByCreatedAtDesc(Long orderId);

  boolean existsByOrder_OrderIdAndStatus(Long orderId, PaymentStatus status);
}
