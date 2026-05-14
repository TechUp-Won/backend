package com.example.WonkaoTalk.domain.order.repo;

import com.example.WonkaoTalk.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepo extends JpaRepository<Order, Long> {

  boolean existsByOrderNumber(String orderNumber);
}
