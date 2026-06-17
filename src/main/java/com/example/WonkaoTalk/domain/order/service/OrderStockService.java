package com.example.WonkaoTalk.domain.order.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStockService {

  private final ProductVariantRepo productVariantRepo;


  // 재고 감소 처리
  public void decreaseStocks(List<OrderItem> orderItems) {
    for (OrderItem orderItem : orderItems) {
      int updatedCount = productVariantRepo.decreaseStockAtomic(
          orderItem.getProductVariant().getId(),
          orderItem.getQuantity()
      );

      if (updatedCount != 1) {
        throw new BusinessException(ErrorCode.PROD_STOCK_UPDATE_FAILED);
      }
    }
  }

  // 재고 증가 처리
  public void increaseStocks(List<OrderItem> orderItems) {
    for (OrderItem orderItem : orderItems) {
      int updatedCount = productVariantRepo.increaseStockAtomic(
          orderItem.getProductVariant().getId(),
          orderItem.getQuantity()
      );
      if (updatedCount != 1) {
        throw new BusinessException(ErrorCode.PROD_STOCK_UPDATE_FAILED);
      }
    }
  }

}
