package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.ProductOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductOptionRepo extends JpaRepository<ProductOption, Long> {

  List<ProductOption> findByProductOptionGroupId(Long productOptionGroupId);

  List<ProductOption> findByProductOptionGroupIdIn(List<Long> groupIds);

  /** 상품의 모든 옵션값(ProductOption.name) — ES 단건 색인용. */
  @Query("SELECT po.name FROM ProductOption po "
      + "WHERE po.productOptionGroup.product.id = :productId")
  List<String> findOptionNamesByProductId(@Param("productId") Long productId);

  /**
   * 여러 상품의 옵션값을 한 번에 조회 — ES 전체 재색인 배치용(N+1 회피).
   * 각 행은 [productId, optionName] 형태로 반환된다.
   */
  @Query("SELECT po.productOptionGroup.product.id, po.name FROM ProductOption po "
      + "WHERE po.productOptionGroup.product.id IN :productIds")
  List<Object[]> findOptionNamesByProductIds(@Param("productIds") List<Long> productIds);
}
