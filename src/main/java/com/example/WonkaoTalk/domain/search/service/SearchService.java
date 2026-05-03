package com.example.WonkaoTalk.domain.search.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.ProductSortType;
import com.example.WonkaoTalk.domain.product.repo.CategoryRepository;
import com.example.WonkaoTalk.domain.product.repo.ProductRepository;
import com.example.WonkaoTalk.domain.search.dto.SearchRequest;
import com.example.WonkaoTalk.domain.search.dto.SearchResponse;
import com.example.WonkaoTalk.domain.search.dto.SearchResponse.ProductResult;
import com.example.WonkaoTalk.domain.search.dto.SearchResponse.StoreInfo;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;

  public SearchResponse search(SearchRequest request) {
    if (request.keyword() == null || request.keyword().isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }

    int size = request.size() != null ? request.size() : 20;
    if (size < 1 || size > 100) {
      throw new BusinessException(ErrorCode.PROD_INVALID_PAGE_SIZE);
    }

    String sortParam = request.sort() != null ? request.sort() : "popular";
    ProductSortType sortType = ProductSortType.from(sortParam);

    if (request.minPrice() != null && request.maxPrice() != null
        && request.minPrice() > request.maxPrice()) {
      throw new BusinessException(ErrorCode.PROD_INVALID_PRICE_RANGE);
    }

    List<Long> categoryIds = null;
    if (request.categoryId() != null) {
      categoryRepository.findById(request.categoryId())
          .orElseThrow(() -> new BusinessException(ErrorCode.PROD_CATEGORY_NOT_FOUND));
      categoryIds = getAllCategoryIds(request.categoryId());
    }

    List<Product> products = productRepository.findWithSearch(
        request.keyword(),
        categoryIds,
        request.minPrice(),
        request.maxPrice(),
        sortType,
        request.lastId(),
        request.lastSortValue(),
        size
    );

    boolean hasNext = products.size() > size;
    if (hasNext) {
      products = products.subList(0, size);
    }

    Long nextCursorId = null;
    Long nextCursorSortValue = null;
    if (hasNext) {
      Product lastItem = products.get(products.size() - 1);
      nextCursorId = lastItem.getId();
      nextCursorSortValue = toSortValue(lastItem, sortType);
    }

    List<ProductResult> productResults = products.stream()
        .map(this::toProductResult)
        .toList();

    // TODO: Store 엔티티 구현 후 storeRepository.findByNameContaining(keyword)로 스토어 검색 결과 반환
    // TODO: ElasticSearch 등 검색 엔진 도입 시 동의어(예: 레드-빨강) 처리 및 스코어 기반 정렬 고도화 필요
    return new SearchResponse(null, productResults, hasNext, nextCursorId, nextCursorSortValue);
  }

  private ProductResult toProductResult(Product product) {
    // TODO: Store 엔티티 구현 시 product.getStore()로 StoreInfo 생성하도록 수정
    return new ProductResult(
        product.getId(),
        product.getName(),
        product.getThumbnail(),
        product.getPrice(),
        product.getDiscountedPrice(),
        product.getDiscountRate(),
        product.getLikeCount(),
        product.getStatus().name(),
        new StoreInfo(product.getStoreId(), null)
    );
  }

  private Long toSortValue(Product product, ProductSortType sortType) {
    return switch (sortType) {
      case POPULAR -> (long) product.getLikeCount();
      case LATEST -> product.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
      case PRICE_ASC, PRICE_DESC -> (long) product.getDiscountedPrice();
    };
  }

  private List<Long> getAllCategoryIds(Long categoryId) {
    List<Long> result = new ArrayList<>();
    result.add(categoryId);
    collectChildIds(categoryId, result);
    return result;
  }

  private void collectChildIds(Long parentId, List<Long> result) {
    List<Category> children = categoryRepository.findByParentCategory_Id(parentId);
    for (Category child : children) {
      result.add(child.getId());
      collectChildIds(child.getId(), result);
    }
  }
}
