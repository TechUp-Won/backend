package com.example.WonkaoTalk.domain.order.event;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderRepo;
import com.example.WonkaoTalk.domain.order.service.OrderStockService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderStockRestoreEventHandler {

  private final OrderRepo orderRepo;
  private final OrderItemRepo orderItemRepo;
  private final OrderStockService orderStockService;

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void handle(OrderStockRestoreRequestEvent event) {
    Order order = orderRepo.findById(event.orderId())
        .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

    List<OrderItem> items = orderItemRepo.findByOrder(order);

    orderStockService.increaseStocks(items);
  }

}
