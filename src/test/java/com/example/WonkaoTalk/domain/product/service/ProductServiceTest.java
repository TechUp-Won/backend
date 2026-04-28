package com.example.WonkaoTalk.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductListRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductListResponse;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductOption;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.entity.VariantOptionMap;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.CategoryRepository;
import com.example.WonkaoTalk.domain.product.repo.ProductDetailRepository;
import com.example.WonkaoTalk.domain.product.repo.ProductImageRepository;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionGroupRepository;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionRepository;
import com.example.WonkaoTalk.domain.product.repo.ProductRepository;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepository;
import com.example.WonkaoTalk.domain.product.repo.VariantOptionMapRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private ProductImageRepository productImageRepository;

  @Mock
  private ProductDetailRepository productDetailRepository;

  @Mock
  private ProductOptionGroupRepository productOptionGroupRepository;

  @Mock
  private ProductOptionRepository productOptionRepository;

  @Mock
  private ProductVariantRepository productVariantRepository;

  @Mock
  private VariantOptionMapRepository variantOptionMapRepository;

  @InjectMocks
  private ProductService productService;

  // ── 입력 검증 ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("페이지 사이즈가 0이면 예외를 던진다")
  void throwsException_whenPageSizeIsZero() {
    ProductListRequest request = defaultRequest();
    request.setSize(0);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productService.getProductList(request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_PAGE_SIZE);
  }

  @Test
  @DisplayName("페이지 사이즈가 101이면 예외를 던진다")
  void throwsException_whenPageSizeExceeds100() {
    ProductListRequest request = defaultRequest();
    request.setSize(101);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productService.getProductList(request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_PAGE_SIZE);
  }

  @Test
  @DisplayName("최소 가격이 최대 가격보다 크면 예외를 던진다")
  void throwsException_whenMinPriceExceedsMaxPrice() {
    ProductListRequest request = defaultRequest();
    request.setMinPrice(10000);
    request.setMaxPrice(5000);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productService.getProductList(request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_PRICE_RANGE);
  }

  @Test
  @DisplayName("존재하지 않는 카테고리 ID면 예외를 던진다")
  void throwsException_whenCategoryNotFound() {
    ProductListRequest request = defaultRequest();
    request.setCategoryId(999L);
    when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productService.getProductList(request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_CATEGORY_NOT_FOUND);
  }

  @Test
  @DisplayName("허용되지 않는 정렬값이면 예외를 던진다")
  void throwsException_whenSortTypeIsInvalid() {
    ProductListRequest request = defaultRequest();
    request.setSort("invalid_sort");

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productService.getProductList(request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_SORT);
  }

  // ── 페이지네이션 ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("결과가 size보다 많으면 hasNext가 true이고 마지막 항목이 제거된다")
  void hasNext_trueAndLastItemRemoved_whenResultsExceedSize() {
    ProductListRequest request = defaultRequest();
    request.setSize(2);

    List<Product> products = mockProducts(3);
    when(productRepository.findWithFilters(any(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(products);

    ProductListResponse response = productService.getProductList(request);

    assertThat(response.isHasNext()).isTrue();
    assertThat(response.getProducts()).hasSize(2);
    assertThat(response.getNextCursorId()).isNotNull();
    assertThat(response.getNextCursorSortValue()).isNotNull();
  }

  @Test
  @DisplayName("결과가 size 이하면 hasNext가 false이고 커서가 null이다")
  void hasNext_falseAndCursorNull_whenResultsWithinSize() {
    ProductListRequest request = defaultRequest();
    request.setSize(5);

    List<Product> products = mockProducts(3);
    when(productRepository.findWithFilters(any(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(products);

    ProductListResponse response = productService.getProductList(request);

    assertThat(response.isHasNext()).isFalse();
    assertThat(response.getNextCursorId()).isNull();
    assertThat(response.getNextCursorSortValue()).isNull();
  }

  // ── 커서 값 계산 ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("POPULAR 정렬의 커서값은 likeCount이다")
  void cursorValue_isLikeCount_whenSortByPopular() {
    ProductListRequest request = defaultRequest();
    request.setSize(1);
    request.setSort("popular");

    Product first = mockProduct(1L, 10000, 20, 8000, 42, LocalDateTime.now());
    Product second = mockProduct(2L, 5000, 0, 5000, 10, LocalDateTime.now());
    when(productRepository.findWithFilters(any(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(List.of(first, second));

    ProductListResponse response = productService.getProductList(request);

    assertThat(response.getNextCursorSortValue()).isEqualTo(42L);
  }

  @Test
  @DisplayName("LATEST 정렬의 커서값은 createdAt의 epoch milliseconds이다")
  void cursorValue_isCreatedAtEpochMillis_whenSortByLatest() {
    LocalDateTime createdAt = LocalDateTime.of(2024, 6, 1, 12, 0, 0);
    long expectedMillis = createdAt.toInstant(ZoneOffset.UTC).toEpochMilli();

    ProductListRequest request = defaultRequest();
    request.setSize(1);
    request.setSort("latest");

    Product first = mockProduct(1L, 10000, 20, 8000, 5, createdAt);
    Product second = mockProduct(2L, 5000, 0, 5000, 3, LocalDateTime.now());
    when(productRepository.findWithFilters(any(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(List.of(first, second));

    ProductListResponse response = productService.getProductList(request);

    assertThat(response.getNextCursorSortValue()).isEqualTo(expectedMillis);
  }

  @Test
  @DisplayName("PRICE 정렬의 커서값은 discountedPrice이다")
  void cursorValue_isDiscountedPrice_whenSortByPrice() {
    ProductListRequest request = defaultRequest();
    request.setSize(1);
    request.setSort("price_asc");

    // price=10000, discountRate=20 → discountedPrice=8000
    Product first = mockProduct(1L, 10000, 20, 8000, 5, LocalDateTime.now());
    Product second = mockProduct(2L, 15000, 0, 15000, 3, LocalDateTime.now());
    when(productRepository.findWithFilters(any(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(List.of(first, second));

    ProductListResponse response = productService.getProductList(request);

    assertThat(response.getNextCursorSortValue()).isEqualTo(8000L);
  }

  // ── getProductDetail ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("존재하지 않는 상품 ID면 PROD_NOT_FOUND 예외를 던진다")
  void throwsException_whenProductIdNotFound() {
    when(productRepository.findById(999L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productService.getProductDetail(999L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_NOT_FOUND);
  }

  @Test
  @DisplayName("논리 삭제된 상품이면 PROD_DELETED 예외를 던진다")
  void throwsException_whenProductIsDeleted() {
    Product product = mockProduct(1L, 10000, 0, 10000, 0, LocalDateTime.now());
    when(product.getDeletedAt()).thenReturn(LocalDateTime.now());
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productService.getProductDetail(1L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_DELETED);
  }

  @Test
  @DisplayName("isLiked는 항상 false를 반환한다")
  void isLiked_isAlwaysFalse() {
    Product product = mockProduct(1L, 10000, 0, 10000, 0, LocalDateTime.now());
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    ProductDetailResponse response = productService.getProductDetail(1L);

    assertThat(response.isLiked()).isFalse();
  }

  @Test
  @DisplayName("store는 항상 null을 반환한다")
  void store_isAlwaysNull() {
    Product product = mockProduct(1L, 10000, 0, 10000, 0, LocalDateTime.now());
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    ProductDetailResponse response = productService.getProductDetail(1L);

    assertThat(response.getStore()).isNull();
  }

  @Test
  @DisplayName("variant의 combinationIds는 VariantOptionMap에 연결된 productOptionId 목록이다")
  void variantCombinationIds_mappedFromVariantOptionMap() {
    Product product = mockProduct(1L, 10000, 0, 10000, 0, LocalDateTime.now());
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    ProductVariant variant = mock(ProductVariant.class);
    when(variant.getId()).thenReturn(10L);
    when(variant.getName()).thenReturn("화이트 / M");
    when(variant.getStock()).thenReturn(50);
    when(variant.getStatus()).thenReturn(SaleStatus.ON_SALE);
    when(productVariantRepository.findByProduct_Id(1L)).thenReturn(List.of(variant));

    ProductOption option1 = mock(ProductOption.class);
    when(option1.getId()).thenReturn(201L);
    ProductOption option2 = mock(ProductOption.class);
    when(option2.getId()).thenReturn(301L);

    VariantOptionMap map1 = mock(VariantOptionMap.class);
    when(map1.getProductVariant()).thenReturn(variant);
    when(map1.getProductOption()).thenReturn(option1);
    VariantOptionMap map2 = mock(VariantOptionMap.class);
    when(map2.getProductVariant()).thenReturn(variant);
    when(map2.getProductOption()).thenReturn(option2);
    when(variantOptionMapRepository.findByProductVariant_IdIn(List.of(10L))).thenReturn(List.of(map1, map2));

    ProductDetailResponse response = productService.getProductDetail(1L);

    assertThat(response.getVariants()).hasSize(1);
    assertThat(response.getVariants().get(0).getCombinationIds()).containsExactlyInAnyOrder(201L, 301L);
  }

  // ── 헬퍼 ────────────────────────────────────────────────────────────────────

  private ProductListRequest defaultRequest() {
    ProductListRequest request = new ProductListRequest();
    request.setSort("popular");
    request.setSize(20);
    return request;
  }

  private Product mockProduct(Long id, int price, int discountRate, int discountedPrice,
      int likeCount, LocalDateTime createdAt) {
    Product product = mock(Product.class);
    when(product.getId()).thenReturn(id);
    when(product.getName()).thenReturn("상품" + id);
    when(product.getPrice()).thenReturn(price);
    when(product.getDiscountRate()).thenReturn(discountRate);
    when(product.getDiscountedPrice()).thenReturn(discountedPrice);
    when(product.getLikeCount()).thenReturn(likeCount);
    when(product.getCreatedAt()).thenReturn(createdAt);
    when(product.getStatus()).thenReturn(SaleStatus.ON_SALE);
    return product;
  }

  private List<Product> mockProducts(int count) {
    List<Product> products = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      products.add(mockProduct((long) (i + 1), 10000, 0, 10000, i, LocalDateTime.now()));
    }
    return products;
  }
}
