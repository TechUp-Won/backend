package com.example.WonkaoTalk.domain.search.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.ProductSortType;
import com.example.WonkaoTalk.domain.product.repo.CategoryRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.search.dto.SearchRequest;
import com.example.WonkaoTalk.domain.search.dto.SearchResponse;
import com.example.WonkaoTalk.domain.search.dto.SearchResponse.ProductResult;
import com.example.WonkaoTalk.domain.search.dto.SearchResponse.StoreInfo;
import com.example.WonkaoTalk.domain.search.dto.SearchResponse.StoreResult;
import com.example.WonkaoTalk.domain.search.repo.ProductSearchQueryRepository;
import com.example.WonkaoTalk.domain.search.repo.ProductSearchResult;
import com.example.WonkaoTalk.domain.store.entity.Store;
import com.example.WonkaoTalk.domain.store.repo.StoreRepo;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

  private final ProductRepo productRepository;
  private final CategoryRepo categoryRepository;
  private final StoreRepo storeRepository;
  private final ProductSearchQueryRepository productSearchQueryRepository;

  /** 상품 검색 엔진 선택: elasticsearch(기본) | database(폴백). */
  @Value("${search.product.engine:elasticsearch}")
  private String searchEngine;

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

    ProductSearchPage productPage = useDatabaseEngine()
        ? searchProductsViaDatabase(request, categoryIds, sortType, size)
        : searchProductsViaElasticsearch(request, categoryIds, sortType, size);

    List<StoreResult> storeResults = storeRepository.findByNameContaining(request.keyword())
        .stream()
        .map(s -> new StoreResult(s.getId(), s.getName(), s.getThumbnail(), s.getDescription()))
        .toList();

    return new SearchResponse(storeResults, productPage.products(), productPage.hasNext(),
        productPage.nextCursorId(), productPage.nextCursorSortValue());
  }

  /** ES: 매칭·정렬·커서로 상품 ID 목록을 산출하고, 상세는 DB 에서 fetch 후 ES 순서로 재정렬(방식 A). */
  private ProductSearchPage searchProductsViaElasticsearch(
      SearchRequest request, List<Long> categoryIds, ProductSortType sortType, int size) {
    ProductSearchResult es = productSearchQueryRepository.search(
        request.keyword(), categoryIds, request.minPrice(), request.maxPrice(),
        sortType, request.lastId(), request.lastSortValue(), size);

    if (es.ids().isEmpty()) {
      return new ProductSearchPage(List.of(), false, null, null);
    }

    Map<Long, Product> byId = productRepository.findWithStoreByIdIn(es.ids()).stream()
        .collect(Collectors.toMap(Product::getId, Function.identity()));

    List<ProductResult> productResults = es.ids().stream()
        .map(byId::get)
        .filter(Objects::nonNull)
        .map(this::toProductResult)
        .toList();

    return new ProductSearchPage(productResults, es.hasNext(), es.nextCursorId(),
        es.nextCursorSortValue());
  }

  /** DB LIKE 폴백 경로 — ES 불가 시 가용성 확보용(search.product.engine=database). */
  private ProductSearchPage searchProductsViaDatabase(
      SearchRequest request, List<Long> categoryIds, ProductSortType sortType, int size) {
    List<Product> products = productRepository.findWithSearch(
        request.keyword(), categoryIds, request.minPrice(), request.maxPrice(),
        sortType, request.lastId(), request.lastSortValue(), size);

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

    return new ProductSearchPage(productResults, hasNext, nextCursorId, nextCursorSortValue);
  }

  private boolean useDatabaseEngine() {
    return "database".equalsIgnoreCase(searchEngine);
  }

  private ProductResult toProductResult(Product product) {
    Store store = product.getStore();
    return new ProductResult(
        product.getId(),
        product.getName(),
        product.getThumbnail(),
        product.getPrice(),
        product.getDiscountedPrice(),
        product.getDiscountRate(),
        product.getLikeCount(),
        product.getStatus().name(),
        new StoreInfo(store.getId(), store.getName())
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
    categoryRepository.findByParentCategoryId(categoryId)
        .forEach(child -> result.add(child.getId()));
    return result;
  }

  /** 상품 검색 결과 페이지(상품 목록 + 커서) — 엔진 공통 내부 표현. */
  private record ProductSearchPage(
      List<ProductResult> products,
      boolean hasNext,
      Long nextCursorId,
      Long nextCursorSortValue
  ) {}
}
