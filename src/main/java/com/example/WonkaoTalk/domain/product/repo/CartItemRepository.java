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

  @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.productVariant pv JOIN FETCH pv.product WHERE ci.id = :id AND ci.cart.user.id = :userId")
  Optional<CartItem> findWithVariantAndProductByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

  Optional<CartItem> findByCartIdAndProductVariantId(Long cartId, Long variantId);

  int countByIdInAndCartId(List<Long> ids, Long cartId);

  @Modifying
  @Query("DELETE FROM CartItem ci WHERE ci.id IN :ids AND ci.cart.id = :cartId")
  void deleteAllByIdInAndCartId(@Param("ids") List<Long> ids, @Param("cartId") Long cartId);

  @Modifying
  @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
  void deleteByCartId(@Param("cartId") Long cartId);
}
