package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.ProductSortType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

  @PersistenceContext
  private EntityManager em;

  @Override
  public List<Product> findWithFilters(
      List<Long> categoryIds,
      Long storeId,
      Integer minPrice,
      Integer maxPrice,
      ProductSortType sortType,
      Long lastProductId,
      Long lastSortValue,
      int size
  ) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Product> cq = cb.createQuery(Product.class);
    Root<Product> p = cq.from(Product.class);

    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.isNull(p.get("deletedAt")));

    // TODO: Store 엔티티 구현 시, N+1 문제 방지를 위해 fetch join(p.fetch("store")) 검토 필요

    if (categoryIds != null && !categoryIds.isEmpty()) {
      predicates.add(p.get("category").get("categoryId").in(categoryIds));
    }
    if (storeId != null) {
      predicates.add(cb.equal(p.get("storeId"), storeId));
    }
    if (minPrice != null) {
      predicates.add(cb.greaterThanOrEqualTo(p.get("price"), minPrice));
    }
    if (maxPrice != null) {
      predicates.add(cb.lessThanOrEqualTo(p.get("price"), maxPrice));
    }
    if (lastProductId != null && lastSortValue != null) {
      predicates.add(buildCursorPredicate(cb, p, sortType, lastProductId, lastSortValue));
    }

    cq.where(predicates.toArray(new Predicate[0]));
    cq.orderBy(buildOrder(cb, p, sortType));

    return em.createQuery(cq)
        .setMaxResults(size + 1)
        .getResultList();
  }

  private Predicate buildCursorPredicate(
      CriteriaBuilder cb, Root<Product> p,
      ProductSortType sortType, Long lastProductId, Long lastSortValue
  ) {
    Predicate tieBreak = cb.lessThan(p.get("productId"), lastProductId);

    return switch (sortType) {
      case POPULAR -> {
        int lastLikeCount = lastSortValue.intValue();
        yield cb.or(
            cb.lessThan(p.<Integer>get("likeCount"), lastLikeCount),
            cb.and(cb.equal(p.get("likeCount"), lastLikeCount), tieBreak)
        );
      }
      case PRICE_DESC -> {
        int lastPrice = lastSortValue.intValue();
        yield cb.or(
            cb.lessThan(p.<Integer>get("price"), lastPrice),
            cb.and(cb.equal(p.get("price"), lastPrice), tieBreak)
        );
      }
      case PRICE_ASC -> {
        int lastPrice = lastSortValue.intValue();
        yield cb.or(
            cb.greaterThan(p.<Integer>get("price"), lastPrice),
            cb.and(cb.equal(p.get("price"), lastPrice), tieBreak)
        );
      }
      case LATEST -> {
        LocalDateTime lastCreatedAt = Instant.ofEpochMilli(lastSortValue)
            .atOffset(ZoneOffset.UTC).toLocalDateTime();
        yield cb.or(
            cb.lessThan(p.<LocalDateTime>get("createdAt"), lastCreatedAt),
            cb.and(cb.equal(p.get("createdAt"), lastCreatedAt), tieBreak)
        );
      }
    };
  }

  private List<Order> buildOrder(CriteriaBuilder cb, Root<Product> p, ProductSortType sortType) {
    Order byProductId = cb.desc(p.get("productId"));
    return switch (sortType) {
      case POPULAR -> List.of(cb.desc(p.get("likeCount")), byProductId);
      case LATEST -> List.of(cb.desc(p.get("createdAt")), byProductId);
      case PRICE_ASC -> List.of(cb.asc(p.get("price")), byProductId);
      case PRICE_DESC -> List.of(cb.desc(p.get("price")), byProductId);
    };
  }
}
