package com.example.WonkaoTalk.domain.search.repo;

import com.example.WonkaoTalk.domain.product.enums.ProductSortType;
import java.util.List;

/**
 * ES 기반 상품 쿼리. 검색(키워드 매칭)과 목록 조회(필터/정렬)를 각각 제공한다.
 * 두 메서드 모두 _source: false 로 ID·커서 값만 수신하고, 표시용 데이터는 DB 에서 fetch 한다.
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

  ProductSearchResult list(
      List<Long> categoryIds,
      Integer minPrice,
      Integer maxPrice,
      ProductSortType sortType,
      Long lastId,
      Long lastSortValue,
      int size
  );
}
