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
    if (lastProductId != null) {
      predicates.add(cb.lessThan(p.get("productId"), lastProductId));
    }

    cq.where(predicates.toArray(new Predicate[0]));
    cq.orderBy(buildOrder(cb, p, sortType));

    return em.createQuery(cq)
        .setMaxResults(size + 1)
        .getResultList();
  }

  private Order buildOrder(CriteriaBuilder cb, Root<Product> p, ProductSortType sortType) {
    return switch (sortType) {
      case LATEST -> cb.desc(p.get("createdAt"));
      case PRICE_ASC -> cb.asc(p.get("price"));
      case PRICE_DESC -> cb.desc(p.get("price"));
      case POPULAR -> cb.desc(p.get("likeCount"));
    };
  }
}
