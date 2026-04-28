package com.example.WonkaoTalk.domain.product.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.product.dto.ProductListRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductListResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductListResponse.ProductSummary;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.ProductSortType;
import com.example.WonkaoTalk.domain.product.repo.CategoryRepository;
import com.example.WonkaoTalk.domain.product.repo.ProductRepository;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;

  public ProductListResponse getProductList(ProductListRequest request) {
    int size = request.getSize() != null ? request.getSize() : 20;
    if (size < 1 || size > 100) {
      throw new BusinessException(ErrorCode.PROD_INVALID_PAGE_SIZE);
    }

    String sortParam = request.getSort() != null ? request.getSort() : "popular";
    ProductSortType sortType = ProductSortType.from(sortParam);

    if (request.getMinPrice() != null && request.getMaxPrice() != null
        && request.getMinPrice() > request.getMaxPrice()) {
      throw new BusinessException(ErrorCode.PROD_INVALID_PRICE_RANGE);
    }

    List<Long> categoryIds = null;
    if (request.getCategoryId() != null) {
      categoryRepository.findById(request.getCategoryId())
          .orElseThrow(() -> new BusinessException(ErrorCode.PROD_CATEGORY_NOT_FOUND));
      categoryIds = getAllCategoryIds(request.getCategoryId());
    }

    List<Product> products = productRepository.findWithFilters(
        categoryIds,
        request.getStoreId(),
        request.getMinPrice(),
        request.getMaxPrice(),
        sortType,
        request.getLastId(),
        request.getLastSortValue(),
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

    List<ProductSummary> summaries = products.stream()
        .map(this::toSummary)
        .toList();

    return ProductListResponse.builder()
        .products(summaries)
        .hasNext(hasNext)
        .nextCursorId(nextCursorId)
        .nextCursorSortValue(nextCursorSortValue)
        .build();
  }

  private Long toSortValue(Product product, ProductSortType sortType) {
    return switch (sortType) {
      case POPULAR -> (long) product.getLikeCount();
      case LATEST -> product.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
      case PRICE_ASC, PRICE_DESC -> (long) product.getPrice();
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

  private ProductSummary toSummary(Product product) {
    return ProductSummary.builder()
        .id(product.getId())
        .name(product.getName())
        .thumbnail(product.getThumbnail())
        .price(product.getPrice())
        .discountedPrice(product.getDiscountedPrice())
        .discountRate(product.getDiscountRate())
        .likeCount(product.getLikeCount())
        .status(product.getStatus().name())
        // TODO: Store 엔티티 구현 시 product.getStore()를 통해 StoreInfo 생성하도록 수정
        .store(null)
        .build();
  }
}
