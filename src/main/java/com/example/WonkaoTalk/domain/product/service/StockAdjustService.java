package com.example.WonkaoTalk.domain.product.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.product.dto.StockAdjustRequest;
import com.example.WonkaoTalk.domain.product.dto.StockAdjustResponse;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.entity.StockHistory;
import com.example.WonkaoTalk.domain.product.enums.StockChangeReason;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import com.example.WonkaoTalk.domain.product.repo.StockHistoryRepo;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.seller.repo.SellerRepo;
import com.example.WonkaoTalk.domain.store.entity.Store;
import com.example.WonkaoTalk.domain.store.repo.StoreRepo;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockAdjustService {

  private static final Set<StockChangeReason> ALLOWED_REASONS =
      Set.of(StockChangeReason.RESTOCK, StockChangeReason.ADJUSTMENT);

  private final SellerRepo sellerRepo;
  private final StoreRepo storeRepo;
  private final ProductRepo productRepo;
  private final ProductVariantRepo productVariantRepo;
  private final StockHistoryRepo stockHistoryRepo;

  @Transactional
  public StockAdjustResponse adjust(Long authId, Long productId, Long variantId,
      StockAdjustRequest request) {
    validateRequest(request);

    Store store = resolveStore(authId);
    Product product = findOwnProduct(productId, store);
    ProductVariant variant = productVariantRepo.findByIdAndProductIdWithLock(variantId, productId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PROD_VARIANT_NOT_FOUND));

    int stockBefore = variant.getStock();
    int newStock = stockBefore + request.changeAmount();
    if (newStock < 0) {
      throw new BusinessException(ErrorCode.PROD_STOCK_INSUFFICIENT);
    }

    variant.adjustStock(request.changeAmount());
    stockHistoryRepo.save(StockHistory.of(variant, null, request.changeAmount(), stockBefore, request.reason()));

    return StockAdjustResponse.builder()
        .variantId(variant.getId())
        .variantName(variant.getName())
        .stockBefore(stockBefore)
        .changeAmount(request.changeAmount())
        .stockAfter(variant.getStock())
        .reason(request.reason())
        .build();
  }

  private void validateRequest(StockAdjustRequest request) {
    if (request.changeAmount() == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }
    if (!ALLOWED_REASONS.contains(request.reason())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }
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
