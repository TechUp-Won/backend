package com.example.WonkaoTalk.domain.product.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailResponse.DetailInfo;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailResponse.ImageInfo;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailResponse.OptionGroupInfo;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailResponse.OptionInfo;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailResponse.VariantInfo;
import com.example.WonkaoTalk.domain.product.dto.ProductListRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductListResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductListResponse.ProductSummary;
import com.example.WonkaoTalk.domain.search.repo.ProductSearchQueryRepository;
import com.example.WonkaoTalk.domain.search.repo.ProductSearchResult;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductOption;
import com.example.WonkaoTalk.domain.product.entity.ProductOptionGroup;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.ProductSortType;
import com.example.WonkaoTalk.domain.product.repo.CategoryRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductDetailRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductImageRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionGroupRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import com.example.WonkaoTalk.domain.product.repo.VariantOptionMapRepo;
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
  private final ProductImageRepo productImageRepository;
  private final ProductDetailRepo productDetailRepository;
  private final ProductOptionGroupRepo productOptionGroupRepository;
  private final ProductOptionRepo productOptionRepository;
  private final ProductVariantRepo productVariantRepository;
  private final VariantOptionMapRepo variantOptionMapRepository;
  private final ProductSearchQueryRepository searchQueryRepository;

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
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PROD_NOT_FOUND));

    if (product.getDeletedAt() != null) {
      throw new BusinessException(ErrorCode.PROD_DELETED);
    }

    List<ImageInfo> images = productImageRepository.findByProductIdOrderBySortOrderAsc(productId)
        .stream()
        .map(img -> ImageInfo.builder()
            .url(img.getUrl())
            .sortOrder(img.getSortOrder())
            .build())
        .toList();

    DetailInfo detail = productDetailRepository.findFirstByProductId(productId)
        .map(d -> DetailInfo.builder().content(d.getContent()).build())
        .orElse(null);

    List<ProductOptionGroup> groups = productOptionGroupRepository.findByProductId(productId);
    List<Long> groupIds = groups.stream().map(ProductOptionGroup::getId).toList();
    Map<Long, List<ProductOption>> optionsByGroup = productOptionRepository
        .findByProductOptionGroupIdIn(groupIds)
        .stream()
        .collect(Collectors.groupingBy(opt -> opt.getProductOptionGroup().getId()));

    List<OptionGroupInfo> optionGroups = groups.stream()
        .map(group -> toOptionGroupInfo(group, optionsByGroup))
        .toList();

    List<ProductVariant> variantList = productVariantRepository.findByProductId(productId);
    List<Long> variantIds = variantList.stream().map(ProductVariant::getId).toList();
    Map<Long, List<Long>> combinationIdsByVariant = variantOptionMapRepository
        .findByProductVariantIdIn(variantIds)
        .stream()
        .collect(Collectors.groupingBy(
            map -> map.getProductVariant().getId(),
            Collectors.mapping(map -> map.getProductOption().getId(), Collectors.toList())
        ));

    List<VariantInfo> variants = variantList.stream()
        .map(variant -> toVariantInfo(variant, combinationIdsByVariant))
        .toList();

    return ProductDetailResponse.builder()
        .productId(product.getId())
        .productName(product.getName())
        .price(product.getPrice())
        .discountedPrice(product.getDiscountedPrice())
        .discountRate(product.getDiscountRate())
        .status(product.getStatus().name())
        .likeCount(product.getLikeCount())
        .isLiked(false) // TODO: 로그인 사용자의 좋아요 여부 반영 필요 (PRODUCT_LIKE 테이블 조회)
        .store(ProductDetailResponse.StoreInfo.builder()
            .storeId(product.getStore().getId())
            .storeName(product.getStore().getName())
            .build())
        .images(images)
        .detail(detail)
        .optionGroups(optionGroups)
        .variants(variants)
        .build();
  }

  private OptionGroupInfo toOptionGroupInfo(ProductOptionGroup group,
      Map<Long, List<ProductOption>> optionsByGroup) {
    List<OptionInfo> options = optionsByGroup.getOrDefault(group.getId(), List.of())
        .stream()
        .map(opt -> OptionInfo.builder()
            .productOptionId(opt.getId())
            .name(opt.getName())
            .build())
        .toList();

    return OptionGroupInfo.builder()
        .productOptionGroupId(group.getId())
        .name(group.getName())
        .options(options)
        .build();
  }

  private VariantInfo toVariantInfo(ProductVariant variant,
      Map<Long, List<Long>> combinationIdsByVariant) {
    List<Long> combinationIds = combinationIdsByVariant.getOrDefault(variant.getId(), List.of());

    return VariantInfo.builder()
        .variantId(variant.getId())
        .variantName(variant.getName())
        .combinationIds(combinationIds)
        .stock(variant.getStock())
        .status(variant.getStatus().name())
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
