package com.example.WonkaoTalk.domain.search.integration;

// TODO: ElasticSearch 도입 시 이 테스트 파일 전체를 재작성해야 합니다.
//       현재는 JPA LIKE 기반의 단순 문자열 포함 검색을 검증합니다.

import static org.assertj.core.api.Assertions.assertThat;

import com.example.WonkaoTalk.config.TestContainerConfig;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.ProductSortType;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.store.entity.Store;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestContainerConfig.class)
@Transactional
class SearchRepositoryCustomImplTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private ProductRepo productRepository;

  private Category category;
  private Store store;

  @BeforeEach
  void setUp() {
    category = saveCategory("테스트카테고리");
    store = saveStore("테스트스토어");
  }

  // ── 키워드 검색 ───────────────────────────────────────────────────────────────

  @Test
  @DisplayName("키워드가 상품명에 포함된 상품만 조회된다")
  void returnsOnlyProductsContainingKeyword() {
    saveProduct("모던 린넨 셔츠", 1L, category, 10000, 0, SaleStatus.ON_SALE, null);
    saveProduct("클래식 데님 팬츠", 1L, category, 20000, 0, SaleStatus.ON_SALE, null);
    flushAndClear();

    List<Product> result = productRepository.findWithSearch(
        "셔츠", null, null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("모던 린넨 셔츠");
  }

  @Test
  @DisplayName("키워드와 일치하는 상품이 없으면 빈 리스트를 반환한다")
  void returnsEmptyList_whenNoProductMatchesKeyword() {
    saveProduct("모던 린넨 셔츠", 1L, category, 10000, 0, SaleStatus.ON_SALE, null);
    flushAndClear();

    List<Product> result = productRepository.findWithSearch(
        "바지", null, null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result).isEmpty();
  }

  // ── 상태 필터 ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("STOP_SALE 상품은 조회되지 않는다")
  void excludesStopSaleProducts() {
    saveProduct("린넨 셔츠", 1L, category, 10000, 0, SaleStatus.ON_SALE, null);
    saveProduct("린넨 재킷", 1L, category, 20000, 0, SaleStatus.STOP_SALE, null);
    flushAndClear();

    List<Product> result = productRepository.findWithSearch(
        "린넨", null, null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("린넨 셔츠");
  }

  @Test
  @DisplayName("OUT_OF_STOCK 상품은 조회된다")
  void includesOutOfStockProducts() {
    saveProduct("린넨 셔츠", 1L, category, 10000, 0, SaleStatus.ON_SALE, null);
    saveProduct("린넨 재킷", 1L, category, 20000, 0, SaleStatus.OUT_OF_STOCK, null);
    flushAndClear();

    List<Product> result = productRepository.findWithSearch(
        "린넨", null, null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("deletedAt이 있는 상품은 조회되지 않는다")
  void excludesDeletedProducts() {
    saveProduct("린넨 셔츠", 1L, category, 10000, 0, SaleStatus.ON_SALE, null);
    saveProduct("린넨 재킷", 1L, category, 20000, 0, SaleStatus.ON_SALE, LocalDateTime.now());
    flushAndClear();

    List<Product> result = productRepository.findWithSearch(
        "린넨", null, null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("린넨 셔츠");
  }

  // ── 완전 일치 가중치 정렬 ──────────────────────────────────────────────────────

  @Test
  @DisplayName("상품명이 키워드와 완전히 일치하는 상품이 likeCount와 무관하게 상단에 노출된다")
  void exactMatchProduct_appearsFirst_regardlessOfLikeCount() {
    saveProduct("린넨 셔츠", 1L, category, 10000, 100, SaleStatus.ON_SALE, null); // 포함 일치, likeCount 높음
    saveProduct("셔츠", 1L, category, 10000, 10, SaleStatus.ON_SALE,
        null);       // 완전 일치, likeCount 낮음
    flushAndClear();

    List<Product> result = productRepository.findWithSearch(
        "셔츠", null, null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("셔츠");
  }

  // ── 가격 필터 ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("minPrice 필터는 discountedPrice 기준으로 동작한다")
  void filtersProductsByMinDiscountedPrice() {
    saveProduct("린넨 셔츠", 1L, category, 10000, 0, SaleStatus.ON_SALE,
        null);  // discountedPrice=10000
    saveProduct("린넨 재킷", 1L, category, 8000, 0, SaleStatus.ON_SALE, null);   // discountedPrice=8000
    flushAndClear();

    List<Product> result = productRepository.findWithSearch(
        "린넨", null, 9000, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDiscountedPrice()).isEqualTo(10000);
  }

  @Test
  @DisplayName("maxPrice 필터는 discountedPrice 기준으로 동작한다")
  void filtersProductsByMaxDiscountedPrice() {
    saveProduct("린넨 셔츠", 1L, category, 10000, 0, SaleStatus.ON_SALE,
        null);  // discountedPrice=10000
    saveProduct("린넨 재킷", 1L, category, 8000, 0, SaleStatus.ON_SALE, null);   // discountedPrice=8000
    flushAndClear();

    List<Product> result = productRepository.findWithSearch(
        "린넨", null, null, 9000, ProductSortType.POPULAR, null, null, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDiscountedPrice()).isEqualTo(8000);
  }

  // ── 카테고리 필터 ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("categoryIds 필터에 해당하는 카테고리의 상품만 조회된다")
  void filtersProductsByCategoryIds() {
    Category other = saveCategory("다른카테고리");
    saveProduct("린넨 셔츠", 1L, category, 10000, 0, SaleStatus.ON_SALE, null);
    saveProduct("린넨 재킷", 1L, other, 20000, 0, SaleStatus.ON_SALE, null);
    flushAndClear();

    List<Product> result = productRepository.findWithSearch(
        "린넨", List.of(category.getId()), null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("린넨 셔츠");
  }

  // ── 커서 페이지네이션 ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("size+1개를 조회하여 다음 페이지 존재 여부를 판단할 수 있다")
  void fetchesSizePlusOne_toDetectNextPage() {
    for (int i = 0; i < 3; i++) {
      saveProduct("린넨 셔츠" + i, 1L, category, 10000, i, SaleStatus.ON_SALE, null);
    }
    flushAndClear();

    List<Product> result = productRepository.findWithSearch(
        "린넨", null, null, null, ProductSortType.POPULAR, null, null, 2);

    assertThat(result).hasSize(3); // size+1 = 3
  }

  @Test
  @DisplayName("POPULAR 커서 이후의 항목만 조회된다")
  void returnsOnlyItemsAfterCursor_whenSortByPopular() {
    saveProduct("린넨 셔츠A", 1L, category, 10000, 100, SaleStatus.ON_SALE, null);
    Product mid = saveProduct("린넨 셔츠B", 1L, category, 10000, 50, SaleStatus.ON_SALE, null);
    saveProduct("린넨 셔츠C", 1L, category, 10000, 10, SaleStatus.ON_SALE, null);
    Long midId = mid.getId();
    flushAndClear();

    List<Product> result = productRepository.findWithSearch(
        "린넨", null, null, null, ProductSortType.POPULAR, midId, 50L, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getLikeCount()).isEqualTo(10);
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

  private Category saveCategory(String name) {
    Category cat = new Category();
    ReflectionTestUtils.setField(cat, "name", name);
    ReflectionTestUtils.setField(cat, "depth", 1);
    em.persist(cat);
    return cat;
  }

  private Store saveStore(String name) {
    Auth auth = Auth.builder().build();
    em.persist(auth);

    Seller seller = Seller.builder()
        .buzNo(String.valueOf(System.nanoTime()).substring(0, 10))
        .name(name + "판매자")
        .phone("010-0000-0000")
        .auth(auth)
        .build();
    em.persist(seller);

    Store s = Store.builder()
        .name(name)
        .description("설명")
        .phone("010-0000-0000")
        .seller(seller)
        .build();
    em.persist(s);
    return s;
  }

  private Product saveProduct(String name, Long ignoredStoreId, Category cat, int discountedPrice,
      int likeCount, SaleStatus status, LocalDateTime deletedAt) {
    Product product = new Product();
    ReflectionTestUtils.setField(product, "name", name);
    ReflectionTestUtils.setField(product, "store", store);
    ReflectionTestUtils.setField(product, "category", cat);
    ReflectionTestUtils.setField(product, "price", discountedPrice);
    ReflectionTestUtils.setField(product, "discountRate", 0);
    ReflectionTestUtils.setField(product, "discountedPrice", discountedPrice);
    ReflectionTestUtils.setField(product, "likeCount", likeCount);
    ReflectionTestUtils.setField(product, "status", status);
    ReflectionTestUtils.setField(product, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(product, "updatedAt", LocalDateTime.now());
    ReflectionTestUtils.setField(product, "deletedAt", deletedAt);
    em.persist(product);
    return product;
  }

  private void flushAndClear() {
    em.flush();
    em.clear();
  }
}
