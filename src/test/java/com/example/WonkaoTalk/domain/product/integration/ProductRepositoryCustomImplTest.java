package com.example.WonkaoTalk.domain.product.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.WonkaoTalk.config.TestContainerConfig;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.ProductSortType;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductRepository;
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
class ProductRepositoryCustomImplTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private ProductRepository productRepository;

  private Category category;

  @BeforeEach
  void setUp() {
    category = saveCategory("테스트카테고리");
  }

  // ── 기본 필터 ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("deletedAt이 있는 상품은 조회되지 않는다")
  void excludesDeletedProducts() {
    saveProduct(1L, category, 10000, null, 0, null);
    saveProduct(1L, category, 8000, null, 0, LocalDateTime.now()); // 삭제됨
    flushAndClear();

    List<Product> result = productRepository.findWithFilters(
        null, null, null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPrice()).isEqualTo(10000);
  }

  @Test
  @DisplayName("storeId로 필터링된다")
  void filtersProductsByStoreId() {
    saveProduct(1L, category, 10000, null, 0, null);
    saveProduct(2L, category, 8000, null, 0, null);
    flushAndClear();

    List<Product> result = productRepository.findWithFilters(
        null, 1L, null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStoreId()).isEqualTo(1L);
  }

  // ── 가격 필터 (할인가 기준) ──────────────────────────────────────────────────

  @Test
  @DisplayName("minPrice 필터는 discountedPrice 기준으로 동작한다")
  void filtersProductsByMinDiscountedPrice() {
    // discountedPrice = 10000 * (100 - 20) / 100 = 8000
    saveProduct(1L, category, 10000, 20, 0, null);
    // discountedPrice = 10000 * (100 - 0) / 100 = 10000
    saveProduct(1L, category, 10000, null, 0, null);
    flushAndClear();

    // minPrice=9000 → discountedPrice 10000짜리만 해당
    List<Product> result = productRepository.findWithFilters(
        null, null, 9000, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDiscountedPrice()).isEqualTo(10000);
  }

  @Test
  @DisplayName("maxPrice 필터는 discountedPrice 기준으로 동작한다")
  void filtersProductsByMaxDiscountedPrice() {
    // discountedPrice = 10000 * (100 - 20) / 100 = 8000
    saveProduct(1L, category, 10000, 20, 0, null);
    // discountedPrice = 10000 * (100 - 0) / 100 = 10000
    saveProduct(1L, category, 10000, null, 0, null);
    flushAndClear();

    // maxPrice=9000 → discountedPrice 8000짜리만 해당
    List<Product> result = productRepository.findWithFilters(
        null, null, null, 9000, ProductSortType.POPULAR, null, null, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDiscountedPrice()).isEqualTo(8000);
  }

  // ── 정렬 (할인가 기준) ───────────────────────────────────────────────────────

  @Test
  @DisplayName("PRICE_ASC 정렬은 discountedPrice 오름차순이다")
  void sortsByDiscountedPriceAscending() {
    // discountedPrice: 7000, 5000, 7200
    saveProduct(1L, category, 10000, 30, 0, null); // 10000 * 70 / 100 = 7000
    saveProduct(1L, category, 5000, null, 0, null); // 5000
    saveProduct(1L, category, 8000, 10, 0, null);  // 8000 * 90 / 100 = 7200
    flushAndClear();

    List<Product> result = productRepository.findWithFilters(
        null, null, null, null, ProductSortType.PRICE_ASC, null, null, 10);

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getDiscountedPrice()).isEqualTo(5000);
    assertThat(result.get(1).getDiscountedPrice()).isEqualTo(7000);
    assertThat(result.get(2).getDiscountedPrice()).isEqualTo(7200);
  }

  @Test
  @DisplayName("PRICE_DESC 정렬은 discountedPrice 내림차순이다")
  void sortsByDiscountedPriceDescending() {
    // discountedPrice: 7000, 5000, 7200
    saveProduct(1L, category, 10000, 30, 0, null); // 7000
    saveProduct(1L, category, 5000, null, 0, null); // 5000
    saveProduct(1L, category, 8000, 10, 0, null);  // 7200
    flushAndClear();

    List<Product> result = productRepository.findWithFilters(
        null, null, null, null, ProductSortType.PRICE_DESC, null, null, 10);

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getDiscountedPrice()).isEqualTo(7200);
    assertThat(result.get(1).getDiscountedPrice()).isEqualTo(7000);
    assertThat(result.get(2).getDiscountedPrice()).isEqualTo(5000);
  }

  // ── 커서 페이지네이션 ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("PRICE_ASC 커서 이후의 항목만 조회된다")
  void returnsOnlyItemsAfterCursor_whenSortByPriceAsc() {
    saveProduct(1L, category, 5000, null, 0, null);  // discountedPrice=5000
    Product mid = saveProduct(1L, category, 8000, null, 0, null);  // discountedPrice=8000
    saveProduct(1L, category, 12000, null, 0, null); // discountedPrice=12000
    Long midId = mid.getId();
    flushAndClear();

    // mid(8000)를 커서로 설정 → 8000 초과 항목만 조회
    List<Product> result = productRepository.findWithFilters(
        null, null, null, null, ProductSortType.PRICE_ASC, midId, 8000L, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDiscountedPrice()).isEqualTo(12000);
  }

  @Test
  @DisplayName("동일한 discountedPrice 내에서 id 내림차순으로 tiebreak된다")
  void tiebreaksById_whenDiscountedPriceIsEqual() {
    Product p1 = saveProduct(1L, category, 5000, null, 0, null);
    Product p2 = saveProduct(1L, category, 5000, null, 0, null);
    saveProduct(1L, category, 5000, null, 0, null);
    Long p2Id = p2.getId();
    flushAndClear();

    // p2를 커서로 설정 → p2보다 id가 작은 항목(p1)만 조회
    List<Product> result = productRepository.findWithFilters(
        null, null, null, null, ProductSortType.PRICE_ASC, p2Id, 5000L, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isLessThan(p2Id);
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

  private Category saveCategory(String name) {
    Category cat = new Category();
    ReflectionTestUtils.setField(cat, "name", name);
    ReflectionTestUtils.setField(cat, "depth", 1);
    em.persist(cat);
    return cat;
  }

  private Product saveProduct(Long storeId, Category cat, int price, Integer discountRate,
      int likeCount, LocalDateTime deletedAt) {
    int discountedPrice = discountRate != null ? price * (100 - discountRate) / 100 : price;
    Product product = new Product();
    ReflectionTestUtils.setField(product, "storeId", storeId);
    ReflectionTestUtils.setField(product, "name", "상품-" + price);
    ReflectionTestUtils.setField(product, "category", cat);
    ReflectionTestUtils.setField(product, "price", price);
    ReflectionTestUtils.setField(product, "discountRate", discountRate);
    ReflectionTestUtils.setField(product, "discountedPrice", discountedPrice);
    ReflectionTestUtils.setField(product, "likeCount", likeCount);
    ReflectionTestUtils.setField(product, "status", SaleStatus.ON_SALE);
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
