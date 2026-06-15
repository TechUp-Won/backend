package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.ProductLike;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductLikeRepo extends JpaRepository<ProductLike, Long> {

  Optional<ProductLike> findByProductIdAndUserId(Long productId, Long userId);

  @Query(value = "SELECT pl FROM ProductLike pl JOIN FETCH pl.product p JOIN FETCH p.store "
      + "WHERE pl.userId = :userId",
      countQuery = "SELECT COUNT(pl) FROM ProductLike pl WHERE pl.userId = :userId")
  Page<ProductLike> findByUserIdWithProduct(@Param("userId") Long userId, Pageable pageable);
}
