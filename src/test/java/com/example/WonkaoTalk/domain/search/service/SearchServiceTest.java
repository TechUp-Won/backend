package com.example.WonkaoTalk.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.CategoryRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.search.dto.SearchRequest;
import com.example.WonkaoTalk.domain.search.dto.SearchResponse;
import com.example.WonkaoTalk.domain.search.repo.ProductSearchQueryRepository;
import com.example.WonkaoTalk.domain.search.repo.ProductSearchResult;
import com.example.WonkaoTalk.domain.store.entity.Store;
import com.example.WonkaoTalk.domain.store.repo.StoreRepo;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchServiceTest {

  @Mock
  private ProductRepo productRepository;

  @Mock
  private CategoryRepo categoryRepository;

  @Mock
  private StoreRepo storeRepository;

  @Mock
  private ProductSearchQueryRepository productSearchQueryRepository;

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

  // ── ES 검색 경로 ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("ES 결과 ID 순서대로 상품을 반환하고 커서를 그대로 전달한다")
  void returnsProductsInElasticsearchOrder_andPassesThroughCursor() {
    SearchRequest request = new SearchRequest("셔츠", null, null, null, "popular", null, null, 2);
    when(productSearchQueryRepository.search(anyString(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(new ProductSearchResult(List.of(2L, 1L), true, 1L, 50L));

    Product p1 = mockProduct(1L, 10000, 8000, 10, LocalDateTime.now());
    Product p2 = mockProduct(2L, 5000, 5000, 50, LocalDateTime.now());
    // DB는 순서를 보장하지 않으므로 ES 순서(2,1)와 다른 순서로 반환
    when(productRepository.findWithStoreByIdIn(List.of(2L, 1L))).thenReturn(List.of(p1, p2));

    SearchResponse response = searchService.search(request);

    assertThat(response.products()).extracting(SearchResponse.ProductResult::id)
        .containsExactly(2L, 1L);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursorId()).isEqualTo(1L);
    assertThat(response.nextCursorSortValue()).isEqualTo(50L);
  }

  @Test
  @DisplayName("ES 결과가 없으면 빈 상품 목록을 반환하고 DB 조회를 하지 않는다")
  void returnsEmpty_whenElasticsearchHasNoMatch() {
    SearchRequest request = defaultRequest("없는상품");
    when(productSearchQueryRepository.search(anyString(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(ProductSearchResult.empty());

    SearchResponse response = searchService.search(request);

    assertThat(response.products()).isEmpty();
    assertThat(response.hasNext()).isFalse();
    assertThat(response.nextCursorId()).isNull();
    verify(productRepository, never()).findWithStoreByIdIn(any());
  }

  @Test
  @DisplayName("stores는 키워드로 검색된 스토어 목록을 반환한다(federated)")
  void stores_returnsMatchingStores() {
    SearchRequest request = defaultRequest("셔츠");
    when(productSearchQueryRepository.search(anyString(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(ProductSearchResult.empty());

    Store store = mock(Store.class);
    when(store.getId()).thenReturn(1L);
    when(store.getName()).thenReturn("셔츠스토어");
    when(store.getThumbnail()).thenReturn("http://thumbnail.png");
    when(store.getDescription()).thenReturn("셔츠 전문점");
    when(storeRepository.findByNameContaining("셔츠")).thenReturn(List.of(store));

    SearchResponse response = searchService.search(request);

    assertThat(response.stores()).hasSize(1);
    assertThat(response.stores().get(0).storeId()).isEqualTo(1L);
    assertThat(response.stores().get(0).storeName()).isEqualTo("셔츠스토어");
  }

  @Test
  @DisplayName("ES 검색 중 예외 발생 시 DB 검색으로 Fallback을 수행한다")
  void fallbacksToDatabase_whenElasticsearchThrowsException() {
    SearchRequest request = new SearchRequest("셔츠", null, null, null, "popular", null, null, 2);
    when(productSearchQueryRepository.search(anyString(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenThrow(new RuntimeException("Elasticsearch connection failed"));

    List<Product> products = mockProducts(2);
    when(productRepository.findWithSearch(anyString(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(products);

    SearchResponse response = searchService.search(request);

    assertThat(response.products()).hasSize(2);
    verify(productRepository).findWithSearch(anyString(), any(), any(), any(), any(), any(), any(), anyInt());
    verify(productRepository, never()).findWithStoreByIdIn(any());
  }

  // ── DB 폴백 경로 (search.product.engine=database) ─────────────────────────────

  @Test
  @DisplayName("DB 폴백: 결과가 size보다 많으면 hasNext가 true이고 마지막 항목이 제거된다")
  void databaseFallback_hasNext_trueAndLastItemRemoved() {
    ReflectionTestUtils.setField(searchService, "searchEngine", "database");
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
  @DisplayName("DB 폴백: POPULAR 정렬의 커서값은 likeCount이다")
  void databaseFallback_cursorValue_isLikeCount_whenSortByPopular() {
    ReflectionTestUtils.setField(searchService, "searchEngine", "database");
    SearchRequest request = new SearchRequest("셔츠", null, null, null, "popular", null, null, 1);

    Product first = mockProduct(1L, 10000, 8000, 42, LocalDateTime.now());
    Product second = mockProduct(2L, 5000, 5000, 10, LocalDateTime.now());
    when(productRepository.findWithSearch(anyString(), any(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(List.of(first, second));

    SearchResponse response = searchService.search(request);

    assertThat(response.nextCursorSortValue()).isEqualTo(42L);
  }

  @Test
  @DisplayName("DB 폴백: LATEST 정렬의 커서값은 createdAt의 epoch milliseconds이다")
  void databaseFallback_cursorValue_isCreatedAtEpochMillis_whenSortByLatest() {
    ReflectionTestUtils.setField(searchService, "searchEngine", "database");
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

  // ── 헬퍼 ────────────────────────────────────────────────────────────────────

  private SearchRequest defaultRequest(String keyword) {
    return new SearchRequest(keyword, null, null, null, "popular", null, null, 20);
  }

  private Product mockProduct(Long id, int price, int discountedPrice, int likeCount,
      LocalDateTime createdAt) {
    Store store = mock(Store.class);
    when(store.getId()).thenReturn(1L);
    when(store.getName()).thenReturn("테스트스토어");

    Product product = mock(Product.class);
    when(product.getId()).thenReturn(id);
    when(product.getName()).thenReturn("상품" + id);
    when(product.getStore()).thenReturn(store);
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
