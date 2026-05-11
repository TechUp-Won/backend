package com.example.WonkaoTalk.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.CategoryRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.search.dto.SearchRequest;
import com.example.WonkaoTalk.domain.search.dto.SearchResponse;
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
class SearchServiceTest {

  @Mock
  private ProductRepo productRepository;

  @Mock
  private CategoryRepo categoryRepository;

  @InjectMocks
  private SearchService searchService;

  // ── keyword 검증 ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("keyword가 null이면 BAD_REQUEST 예외를 던진다")
  void throwsException_whenKeywordIsNull() {
    SearchRequest request = defaultRequest(null);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> searchService.search(request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  @DisplayName("keyword가 빈 문자열이면 BAD_REQUEST 예외를 던진다")
  void throwsException_whenKeywordIsBlank() {
    SearchRequest request = defaultRequest("   ");

    BusinessException ex = assertThrows(BusinessException.class,
        () -> searchService.search(request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  // ── 입력 검증 ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("페이지 사이즈가 0이면 예외를 던진다")
  void throwsException_whenPageSizeIsZero() {
    SearchRequest request = new SearchRequest("셔츠", null, null, null, "popular", null, null, 0);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> searchService.search(request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_PAGE_SIZE);
  }

  @Test
  @DisplayName("페이지 사이즈가 101이면 예외를 던진다")
  void throwsException_whenPageSizeExceeds100() {
    SearchRequest request = new SearchRequest("셔츠", null, null, null, "popular", null, null, 101);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> searchService.search(request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_PAGE_SIZE);
  }

  @Test
  @DisplayName("허용되지 않는 정렬값이면 예외를 던진다")
  void throwsException_whenSortTypeIsInvalid() {
    SearchRequest request = new SearchRequest("셔츠", null, null, null, "invalid_sort", null, null, 20);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> searchService.search(request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_SORT);
  }

  @Test
  @DisplayName("최소 가격이 최대 가격보다 크면 예외를 던진다")
  void throwsException_whenMinPriceExceedsMaxPrice() {
    SearchRequest request = new SearchRequest("셔츠", null, 10000, 5000, "popular", null, null, 20);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> searchService.search(request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_PRICE_RANGE);
  }

  @Test
  @DisplayName("존재하지 않는 카테고리 ID면 예외를 던진다")
  void throwsException_whenCategoryNotFound() {
    SearchRequest request = new SearchRequest("셔츠", 999L, null, null, "popular", null, null, 20);
    when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> searchService.search(request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_CATEGORY_NOT_FOUND);
  }

  // ── 페이지네이션 ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("결과가 size보다 많으면 hasNext가 true이고 마지막 항목이 제거된다")
  void hasNext_trueAndLastItemRemoved_whenResultsExceedSize() {
    SearchRequest request = new SearchRequest("셔츠", null, null, null, "popular", null, null, 2);
    List<Product> products = mockProducts(3);
    when(productRepository.findWithSearch(anyString(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(products);

    SearchResponse response = searchService.search(request);

    assertThat(response.hasNext()).isTrue();
    assertThat(response.products()).hasSize(2);
    assertThat(response.nextCursorId()).isNotNull();
    assertThat(response.nextCursorSortValue()).isNotNull();
  }

  @Test
  @DisplayName("결과가 size 이하면 hasNext가 false이고 커서가 null이다")
  void hasNext_falseAndCursorNull_whenResultsWithinSize() {
    SearchRequest request = new SearchRequest("셔츠", null, null, null, "popular", null, null, 5);
    List<Product> products = mockProducts(3);
    when(productRepository.findWithSearch(anyString(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(products);

    SearchResponse response = searchService.search(request);

    assertThat(response.hasNext()).isFalse();
    assertThat(response.nextCursorId()).isNull();
    assertThat(response.nextCursorSortValue()).isNull();
  }

  // ── 커서 값 계산 ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("POPULAR 정렬의 커서값은 likeCount이다")
  void cursorValue_isLikeCount_whenSortByPopular() {
    SearchRequest request = new SearchRequest("셔츠", null, null, null, "popular", null, null, 1);

    Product first = mockProduct(1L, 10000, 8000, 42, LocalDateTime.now());
    Product second = mockProduct(2L, 5000, 5000, 10, LocalDateTime.now());
    when(productRepository.findWithSearch(anyString(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(List.of(first, second));

    SearchResponse response = searchService.search(request);

    assertThat(response.nextCursorSortValue()).isEqualTo(42L);
  }

  @Test
  @DisplayName("LATEST 정렬의 커서값은 createdAt의 epoch milliseconds이다")
  void cursorValue_isCreatedAtEpochMillis_whenSortByLatest() {
    LocalDateTime createdAt = LocalDateTime.of(2024, 6, 1, 12, 0, 0);
    long expectedMillis = createdAt.toInstant(ZoneOffset.UTC).toEpochMilli();

    SearchRequest request = new SearchRequest("셔츠", null, null, null, "latest", null, null, 1);

    Product first = mockProduct(1L, 10000, 8000, 5, createdAt);
    Product second = mockProduct(2L, 5000, 5000, 3, LocalDateTime.now());
    when(productRepository.findWithSearch(anyString(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(List.of(first, second));

    SearchResponse response = searchService.search(request);

    assertThat(response.nextCursorSortValue()).isEqualTo(expectedMillis);
  }

  @Test
  @DisplayName("PRICE 정렬의 커서값은 discountedPrice이다")
  void cursorValue_isDiscountedPrice_whenSortByPrice() {
    SearchRequest request = new SearchRequest("셔츠", null, null, null, "price_asc", null, null, 1);

    Product first = mockProduct(1L, 10000, 8000, 5, LocalDateTime.now());
    Product second = mockProduct(2L, 15000, 15000, 3, LocalDateTime.now());
    when(productRepository.findWithSearch(anyString(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(List.of(first, second));

    SearchResponse response = searchService.search(request);

    assertThat(response.nextCursorSortValue()).isEqualTo(8000L);
  }

  // ── 응답 구조 ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("stores는 항상 null이다")
  void stores_isAlwaysNull() {
    SearchRequest request = defaultRequest("셔츠");
    when(productRepository.findWithSearch(anyString(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(List.of());

    SearchResponse response = searchService.search(request);

    assertThat(response.stores()).isNull();
  }

  // ── 헬퍼 ────────────────────────────────────────────────────────────────────

  private SearchRequest defaultRequest(String keyword) {
    return new SearchRequest(keyword, null, null, null, "popular", null, null, 20);
  }

  private Product mockProduct(Long id, int price, int discountedPrice, int likeCount,
      LocalDateTime createdAt) {
    Product product = mock(Product.class);
    when(product.getId()).thenReturn(id);
    when(product.getName()).thenReturn("상품" + id);
    when(product.getStoreId()).thenReturn(1L);
    when(product.getPrice()).thenReturn(price);
    when(product.getDiscountedPrice()).thenReturn(discountedPrice);
    when(product.getDiscountRate()).thenReturn(0);
    when(product.getLikeCount()).thenReturn(likeCount);
    when(product.getCreatedAt()).thenReturn(createdAt);
    when(product.getStatus()).thenReturn(SaleStatus.ON_SALE);
    return product;
  }

  private List<Product> mockProducts(int count) {
    List<Product> products = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      products.add(mockProduct((long) (i + 1), 10000, 10000, i, LocalDateTime.now()));
    }
    return products;
  }
}
