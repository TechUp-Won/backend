package com.example.WonkaoTalk.domain.order.repo;

import com.example.WonkaoTalk.domain.order.entity.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepo extends JpaRepository<Order, Long> {

  boolean existsByOrderNumber(String orderNumber);

  Page<Order> findByUserId(Long userId, Pageable pageable);

  Optional<Order> findByUserIdAndOrderId(Long userId, Long orderId);
}
