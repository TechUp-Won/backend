package com.example.WonkaoTalk.domain.product.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.order.entity.OrderStatus;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.event.ProductIndexRequestedEvent;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.seller.repo.SellerRepo;
import com.example.WonkaoTalk.domain.store.entity.Store;
import com.example.WonkaoTalk.domain.store.repo.StoreRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductDeleteService {

  private static final List<OrderStatus> ACTIVE_STATUSES =
      List.of(OrderStatus.CREATED, OrderStatus.PAYMENT_PENDING, OrderStatus.PAID);

  private final SellerRepo sellerRepo;
  private final StoreRepo storeRepo;
  private final ProductRepo productRepo;
  private final OrderItemRepo orderItemRepo;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void delete(Long authId, Long productId) {
    Store store = resolveStore(authId);
    Product product = findOwnProduct(productId, store);

    if (orderItemRepo.existsByProductVariant_Product_IdAndOrder_OrderStatusIn(productId, ACTIVE_STATUSES)) {
      throw new BusinessException(ErrorCode.PROD_HAS_ACTIVE_ORDER);
    }

    product.softDelete();
    eventPublisher.publishEvent(ProductIndexRequestedEvent.delete(productId));
  }

  private Store resolveStore(Long authId) {
    Seller seller = sellerRepo.findByAuthId(authId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));
    return storeRepo.findBySeller(seller)
        .orElseThrow(() -> new BusinessException(ErrorCode.PROD_STORE_NOT_FOUND));
  }

  private Product findOwnProduct(Long productId, Store store) {
    Product product = productRepo.findByIdWithLock(productId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PROD_NOT_FOUND));
    if (product.getDeletedAt() != null) {
      throw new BusinessException(ErrorCode.PROD_DELETED);
    }
    if (!product.getStore().getId().equals(store.getId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return product;
  }
}
