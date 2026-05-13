package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepo extends JpaRepository<ProductVariant, Long> {

  List<ProductVariant> findByProductId(Long productId);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE ProductVariant pv SET pv.stock = pv.stock - :quantity " +
      "WHERE pv.id = :id AND pv.stock >= :quantity")
  int decreaseStockAtomic(@Param("id") Long id, @Param("quantity") int quantity);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE ProductVariant pv SET pv.stock = pv.stock + :quantity " +
      "WHERE pv.id = :id")
  void increaseStockAtomic(@Param("id") Long id, @Param("quantity") int quantity);
}
