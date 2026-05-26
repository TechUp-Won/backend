package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepo extends JpaRepository<ProductVariant, Long> {

  List<ProductVariant> findByProductId(Long productId);

  Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT pv FROM ProductVariant pv WHERE pv.id = :id AND pv.product.id = :productId")
  Optional<ProductVariant> findByIdAndProductIdWithLock(
      @Param("id") Long id,
      @Param("productId") Long productId
  );

  @Modifying(clearAutomatically = true)
  @Query("UPDATE ProductVariant pv " +
      "SET pv.stock = pv.stock - :quantity, " +
      "pv.status = CASE WHEN (pv.stock - :quantity) = 0 AND pv.status = :onSale THEN :outOfStock ELSE pv.status END "
      +
      "WHERE pv.id = :id AND pv.stock >= :quantity")
  int decreaseStock(
      @Param("id") Long id,
      @Param("quantity") int quantity,
      @Param("onSale") SaleStatus onSale,
      @Param("outOfStock") SaleStatus outOfStock
  );

  @Modifying(clearAutomatically = true)
  @Query("UPDATE ProductVariant pv " +
      "SET pv.stock = pv.stock + :quantity, " +
      "pv.status = CASE WHEN pv.status = :outOfStock THEN :onSale ELSE pv.status END " +
      "WHERE pv.id = :id")
  int increaseStock(
      @Param("id") Long id,
      @Param("quantity") int quantity,
      @Param("outOfStock") SaleStatus outOfStock,
      @Param("onSale") SaleStatus onSale
  );

  default int decreaseStockAtomic(Long id, int quantity) {
    return decreaseStock(id, quantity, SaleStatus.ON_SALE, SaleStatus.OUT_OF_STOCK);
  }

  default int increaseStockAtomic(Long id, int quantity) {
    return increaseStock(id, quantity, SaleStatus.OUT_OF_STOCK, SaleStatus.ON_SALE);
  }
}
