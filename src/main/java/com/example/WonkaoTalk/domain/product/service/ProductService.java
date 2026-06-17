package com.example.WonkaoTalk.domain.product.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailCacheDto;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailResponse.VariantInfo;
import com.example.WonkaoTalk.domain.product.dto.ProductListRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductListResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductListResponse.ProductSummary;
import com.example.WonkaoTalk.domain.product.dto.VariantStockDto;
import com.example.WonkaoTalk.domain.search.repo.ProductSearchQueryRepository;
import com.example.WonkaoTalk.domain.search.repo.ProductSearchResult;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.ProductSortType;
import com.example.WonkaoTalk.domain.product.repo.CategoryRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

  private final ProductRepo productRepository;
  private final CategoryRepo categoryRepository;
  private final ProductVariantRepo productVariantRepository;
  private final ProductSearchQueryRepository searchQueryRepository;
  private final ProductCacheService productCacheService;

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

    if (request.getStoreId() != null) {
      return getProductListFromDb(categoryIds, request.getStoreId(), request.getMinPrice(),
          request.getMaxPrice(), sortType, request.getLastId(), request.getLastSortValue(), size);
    }

    try {
      ProductSearchResult esResult = searchQueryRepository.list(
          categoryIds, request.getMinPrice(), request.getMaxPrice(),
          sortType, request.getLastId(), request.getLastSortValue(), size);
      return buildEsResponse(esResult);
    } catch (Exception e) {
      return getProductListFromDb(categoryIds, null, request.getMinPrice(),
          request.getMaxPrice(), sortType, request.getLastId(), request.getLastSortValue(), size);
    }
  }

  private ProductListResponse getProductListFromDb(
      List<Long> categoryIds, Long storeId, Integer minPrice, Integer maxPrice,
      ProductSortType sortType, Long lastId, Long lastSortValue, int size
  ) {
    List<Product> products = productRepository.findWithFilters(
        categoryIds, storeId, minPrice, maxPrice, sortType, lastId, lastSortValue, size);

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

    return ProductListResponse.builder()
        .products(products.stream().map(this::toSummary).toList())
        .hasNext(hasNext)
        .nextCursorId(nextCursorId)
        .nextCursorSortValue(nextCursorSortValue)
        .build();
  }

  private ProductListResponse buildEsResponse(ProductSearchResult esResult) {
    if (esResult.ids().isEmpty()) {
      return ProductListResponse.builder()
          .products(List.of())
          .hasNext(false)
          .nextCursorId(null)
          .nextCursorSortValue(null)
          .build();
    }

    List<Product> products = productRepository.findWithStoreByIdIn(esResult.ids());
    Map<Long, Product> productMap = products.stream()
        .collect(Collectors.toMap(Product::getId, p -> p));

    List<ProductSummary> summaries = esResult.ids().stream()
        .map(productMap::get)
        .filter(Objects::nonNull)
        .map(this::toSummary)
        .toList();

    return ProductListResponse.builder()
        .products(summaries)
        .hasNext(esResult.hasNext())
        .nextCursorId(esResult.nextCursorId())
        .nextCursorSortValue(esResult.nextCursorSortValue())
        .build();
  }

  public ProductDetailResponse getProductDetail(Long productId) {
    ProductDetailCacheDto cached = productCacheService.getProductDetailBase(productId);

    Map<Long, Integer> stockMap = productVariantRepository.findStocksByProductId(productId)
        .stream()
        .collect(Collectors.toMap(VariantStockDto::variantId, VariantStockDto::stock));

    List<VariantInfo> variants = cached.getVariants().stream()
        .map(v -> VariantInfo.builder()
            .variantId(v.getVariantId())
            .variantName(v.getVariantName())
            .combinationIds(v.getCombinationIds())
            .stock(stockMap.getOrDefault(v.getVariantId(), 0))
            .status(v.getStatus())
            .build())
        .toList();

    return ProductDetailResponse.builder()
        .productId(cached.getProductId())
        .productName(cached.getProductName())
        .price(cached.getPrice())
        .discountedPrice(cached.getDiscountedPrice())
        .discountRate(cached.getDiscountRate())
        .status(cached.getStatus())
        .likeCount(cached.getLikeCount())
        .isLiked(cached.isLiked())
        .store(ProductDetailResponse.StoreInfo.builder()
            .storeId(cached.getStore().getStoreId())
            .storeName(cached.getStore().getStoreName())
            .build())
        .images(cached.getImages().stream()
            .map(img -> ProductDetailResponse.ImageInfo.builder()
                .url(img.getUrl())
                .sortOrder(img.getSortOrder())
                .build())
            .toList())
        .detail(cached.getDetail() != null
            ? ProductDetailResponse.DetailInfo.builder()
                .content(cached.getDetail().getContent())
                .build()
            : null)
        .optionGroups(cached.getOptionGroups().stream()
            .map(g -> ProductDetailResponse.OptionGroupInfo.builder()
                .productOptionGroupId(g.getProductOptionGroupId())
                .name(g.getName())
                .options(g.getOptions().stream()
                    .map(o -> ProductDetailResponse.OptionInfo.builder()
                        .productOptionId(o.getProductOptionId())
                        .name(o.getName())
                        .build())
                    .toList())
                .build())
            .toList())
        .variants(variants)
        .build();
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
        .store(ProductListResponse.StoreInfo.builder()
            .storeId(product.getStore().getId())
            .storeName(product.getStore().getName())
            .build())
        .build();
  }
}
