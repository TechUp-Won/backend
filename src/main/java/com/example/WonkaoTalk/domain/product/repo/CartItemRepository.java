package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

  @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.productVariant pv JOIN FETCH pv.product WHERE ci.cart.id = :cartId")
  List<CartItem> findAllWithVariantAndProductByCartId(@Param("cartId") Long cartId);

  @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.productVariant pv JOIN FETCH pv.product WHERE ci.id = :id")
  Optional<CartItem> findWithVariantAndProductById(@Param("id") Long id);

  Optional<CartItem> findByCart_IdAndProductVariant_Id(Long cartId, Long variantId);

  List<CartItem> findAllByIdInAndCart_Id(List<Long> ids, Long cartId);

  @Modifying
  @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
  void deleteByCart_Id(@Param("cartId") Long cartId);
}
