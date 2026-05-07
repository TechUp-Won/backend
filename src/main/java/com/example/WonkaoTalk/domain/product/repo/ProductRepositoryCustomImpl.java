package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.ProductSortType;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
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
      Long lastId,
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
      predicates.add(p.get("category").get("id").in(categoryIds));
    }
    if (storeId != null) {
      predicates.add(cb.equal(p.get("storeId"), storeId));
    }
    if (minPrice != null) {
      predicates.add(cb.greaterThanOrEqualTo(p.get("discountedPrice"), minPrice));
    }
    if (maxPrice != null) {
      predicates.add(cb.lessThanOrEqualTo(p.get("discountedPrice"), maxPrice));
    }
    if (lastId != null && lastSortValue != null) {
      predicates.add(buildCursorPredicate(cb, p, sortType, lastId, lastSortValue));
    }

    cq.where(predicates.toArray(new Predicate[0]));
    cq.orderBy(buildOrder(cb, p, sortType));

    return em.createQuery(cq)
        .setMaxResults(size + 1)
        .getResultList();
  }

  @Override
  public List<Product> findWithSearch(
      String keyword,
      List<Long> categoryIds,
      Integer minPrice,
      Integer maxPrice,
      ProductSortType sortType,
      Long lastId,
      Long lastSortValue,
      int size
  ) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Product> cq = cb.createQuery(Product.class);
    Root<Product> p = cq.from(Product.class);

    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.isNull(p.get("deletedAt")));
    predicates.add(cb.notEqual(p.get("status"), SaleStatus.STOP_SALE));

    // TODO: Store 엔티티 구현 시, N+1 문제 방지를 위해 fetch join(p.fetch("store")) 검토 필요
    // TODO: ElasticSearch 등 검색 엔진 도입 시 동의어(예: 레드-빨강) 처리 및 스코어 기반 정렬로 교체 필요

    predicates.add(cb.like(p.get("name"), "%" + keyword + "%"));

    if (categoryIds != null && !categoryIds.isEmpty()) {
      predicates.add(p.get("category").get("id").in(categoryIds));
    }
    if (minPrice != null) {
      predicates.add(cb.greaterThanOrEqualTo(p.get("discountedPrice"), minPrice));
    }
    if (maxPrice != null) {
      predicates.add(cb.lessThanOrEqualTo(p.get("discountedPrice"), maxPrice));
    }
    if (lastId != null && lastSortValue != null) {
      predicates.add(buildCursorPredicate(cb, p, sortType, lastId, lastSortValue));
    }

    cq.where(predicates.toArray(new Predicate[0]));

    // 상품명 완전 일치 결과를 상단에 노출하고, 그 외는 정렬 기준에 따라 정렬
    Expression<Integer> exactMatchWeight = cb.<Integer>selectCase()
        .when(cb.equal(p.get("name"), keyword), 0)
        .otherwise(1);
    List<Order> orders = new ArrayList<>();
    orders.add(cb.asc(exactMatchWeight));
    orders.addAll(buildOrder(cb, p, sortType));
    cq.orderBy(orders);

    return em.createQuery(cq)
        .setMaxResults(size + 1)
        .getResultList();
  }

  private Predicate buildCursorPredicate(
      CriteriaBuilder cb, Root<Product> p,
      ProductSortType sortType, Long lastId, Long lastSortValue
  ) {
    Predicate tieBreak = cb.lessThan(p.get("id"), lastId);

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
            cb.lessThan(p.<Integer>get("discountedPrice"), lastPrice),
            cb.and(cb.equal(p.get("discountedPrice"), lastPrice), tieBreak)
        );
      }
      case PRICE_ASC -> {
        int lastPrice = lastSortValue.intValue();
        yield cb.or(
            cb.greaterThan(p.<Integer>get("discountedPrice"), lastPrice),
            cb.and(cb.equal(p.get("discountedPrice"), lastPrice), tieBreak)
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
    Order byId = cb.desc(p.get("id"));
    return switch (sortType) {
      case POPULAR -> List.of(cb.desc(p.get("likeCount")), byId);
      case LATEST -> List.of(cb.desc(p.get("createdAt")), byId);
      case PRICE_ASC -> List.of(cb.asc(p.get("discountedPrice")), byId);
      case PRICE_DESC -> List.of(cb.desc(p.get("discountedPrice")), byId);
    };
  }
}
