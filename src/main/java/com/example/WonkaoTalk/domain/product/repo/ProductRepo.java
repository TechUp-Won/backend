package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepo extends JpaRepository<Product, Long>, ProductRepoCustom {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Product p WHERE p.id = :id")
  Optional<Product> findByIdWithLock(@Param("id") Long id);

  /** ES 검색 결과 ID 목록으로 Store 까지 fetch (방식 A). 정렬은 호출 측에서 ES 순서로 재정렬. */
  @Query("SELECT p FROM Product p JOIN FETCH p.store WHERE p.id IN :ids")
  List<Product> findWithStoreByIdIn(@Param("ids") List<Long> ids);

  /**
   * 색인 대상(삭제되지 않고 판매상태가 노출 대상) 상품을 id 키셋 페이징으로 조회 — 전체 재색인 배치용.
   * category 를 fetch join 하여 N+1 을 피한다. {@code lastId} 이후부터 {@code pageable} 크기만큼 반환.
   */
  @Query("SELECT p FROM Product p JOIN FETCH p.category "
      + "WHERE p.deletedAt IS NULL AND p.status IN :statuses AND p.id > :lastId "
      + "ORDER BY p.id")
  List<Product> findIndexableForReindex(
      @Param("statuses") List<SaleStatus> statuses,
      @Param("lastId") Long lastId,
      Pageable pageable);
}
