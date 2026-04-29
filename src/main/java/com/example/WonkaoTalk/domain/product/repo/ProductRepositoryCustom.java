package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.ProductSortType;
import java.util.List;

public interface ProductRepositoryCustom {

  List<Product> findWithFilters(
      List<Long> categoryIds,
      Long storeId,
      Integer minPrice,
      Integer maxPrice,
      ProductSortType sortType,
      Long lastId,
      Long lastSortValue,
      int size
  );
}
