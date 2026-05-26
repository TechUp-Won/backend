package com.example.WonkaoTalk.domain.order.repo;

import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import com.example.WonkaoTalk.domain.order.entity.OrderStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepo extends JpaRepository<OrderItem, Long> {

  List<OrderItem> findByOrder(Order order);

  @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
      "JOIN oi.order o " +
      "WHERE oi.productVariant.product.id = :productId " +
      "AND o.orderStatus IN :statuses")
  boolean existsActiveOrderByProductId(
      @Param("productId") Long productId,
      @Param("statuses") List<OrderStatus> statuses
  );
}
