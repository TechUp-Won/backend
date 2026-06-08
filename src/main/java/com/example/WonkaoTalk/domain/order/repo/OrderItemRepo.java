package com.example.WonkaoTalk.domain.order.repo;

import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import com.example.WonkaoTalk.domain.order.entity.OrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepo extends JpaRepository<OrderItem, Long> {

  List<OrderItem> findByOrder(Order order);

  boolean existsByProductVariant_Product_IdAndOrder_OrderStatusIn(
      Long productId,
      List<OrderStatus> statuses
  );

  @Query("SELECT DISTINCT oi.productImageUrl FROM OrderItem oi WHERE oi.productImageUrl IN :urls")
  Set<String> findReferencedImageUrls(@Param("urls") Collection<String> urls);
}
