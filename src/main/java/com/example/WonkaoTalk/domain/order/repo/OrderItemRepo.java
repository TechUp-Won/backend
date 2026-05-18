package com.example.WonkaoTalk.domain.order.repo;

import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepo extends JpaRepository<OrderItem, Long> {

  List<OrderItem> findByOrder(Order order);
}
