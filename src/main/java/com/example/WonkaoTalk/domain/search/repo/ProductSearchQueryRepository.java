package com.example.WonkaoTalk.domain.search.repo;

import com.example.WonkaoTalk.domain.product.enums.ProductSortType;
import java.util.List;

/**
 * ES 기반 상품 검색 쿼리. 상품명 + 옵션값을 통합 매칭하고 필터/정렬/커서를 적용하여
 * 상품 ID 목록과 다음 커서를 산출한다.
 */
public interface ProductSearchQueryRepository {

  ProductSearchResult search(
      String keyword,
      List<Long> categoryIds,
      Integer minPrice,
      Integer maxPrice,
      ProductSortType sortType,
      Long lastId,
      Long lastSortValue,
      int size
  );
}
