package com.example.WonkaoTalk.domain.product.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.WonkaoTalk.config.TestContainerConfig;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.store.entity.Store;
import jakarta.persistence.EntityManager;
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
class ProductVariantRepoTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private ProductVariantRepo productVariantRepo;

  private ProductVariant variant;

  @BeforeEach
  void setUp() {
    Category category = saveCategory("테스트카테고리");
    Store store = saveStore("테스트스토어");
    Product product = saveProduct(store, category, "테스트상품", 10000);
    variant = saveVariant(product, "테스트옵션", 10);
    flushAndClear();
  }

  // ── 재고 관리 (Atomic) ───────────────────────────────────────────────────────

  @Test
  @DisplayName("재고가 0이 되면 상태가 OUT_OF_STOCK으로 변경된다")
  void decreaseStockAtomic_ChangesStatusToOutOfStock_WhenStockHitsZero() {
    // when
    int updatedCount = productVariantRepo.decreaseStockAtomic(variant.getId(), 10);

    // then
    assertThat(updatedCount).isEqualTo(1);

    ProductVariant updated = productVariantRepo.findById(variant.getId()).orElseThrow();
    assertThat(updated.getStock()).isEqualTo(0);
    assertThat(updated.getStatus()).isEqualTo(SaleStatus.OUT_OF_STOCK);
  }

  @Test
  @DisplayName("OUT_OF_STOCK 상태에서 재고가 증가하면 ON_SALE로 변경된다")
  void increaseStockAtomic_ChangesStatusToOnSale_WhenPreviouslyOutOfStock() {
    // given (재고를 0으로 만들어 OUT_OF_STOCK 상태로 변경)
    productVariantRepo.decreaseStockAtomic(variant.getId(), 10);
    flushAndClear();

    // when
    productVariantRepo.increaseStockAtomic(variant.getId(), 5);

    // then
    ProductVariant updated = productVariantRepo.findById(variant.getId()).orElseThrow();
    assertThat(updated.getStock()).isEqualTo(5);
    assertThat(updated.getStatus()).isEqualTo(SaleStatus.ON_SALE);
  }

  @Test
  @DisplayName("STOP_SALE 상태에서는 재고가 증가해도 상태가 변경되지 않는다")
  void increaseStockAtomic_DoesNotChangeStatus_WhenStopSale() {
    // given (상태를 STOP_SALE로 변경)
    ProductVariant v = productVariantRepo.findById(variant.getId()).orElseThrow();
    ReflectionTestUtils.setField(v, "status", SaleStatus.STOP_SALE);
    em.persist(v);
    flushAndClear();

    // when
    productVariantRepo.increaseStockAtomic(variant.getId(), 5);

    // then
    ProductVariant updated = productVariantRepo.findById(variant.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(SaleStatus.STOP_SALE);
  }
  @Test
  @DisplayName("재고가 부족하면 감소시키지 않고 0을 반환한다")
  void decreaseStockAtomic_Fail_InsufficientStock() {
    // when
    int updatedCount = productVariantRepo.decreaseStockAtomic(variant.getId(), 11);

    // then
    assertThat(updatedCount).isEqualTo(0);

    ProductVariant updated = productVariantRepo.findById(variant.getId()).orElseThrow();
    assertThat(updated.getStock()).isEqualTo(10);
  }

  @Test
  @DisplayName("재고를 성공적으로 증가시킨다")
  void increaseStockAtomic_Success() {
    // when
    productVariantRepo.increaseStockAtomic(variant.getId(), 5);

    // then
    ProductVariant updated = productVariantRepo.findById(variant.getId()).orElseThrow();
    assertThat(updated.getStock()).isEqualTo(15);
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

    Store store = Store.builder()
        .name(name)
        .description("설명")
        .phone("010-0000-0000")
        .seller(seller)
        .build();
    em.persist(store);
    return store;
  }

  private Product saveProduct(Store store, Category cat, String name, int price) {
    Product product = Product.builder()
        .store(store)
        .name(name)
        .category(cat)
        .price(price)
        .discountedPrice(price)
        .status(SaleStatus.ON_SALE)
        .likeCount(0)
        .build();
    em.persist(product);
    return product;
  }

  private ProductVariant saveVariant(Product product, String name, int stock) {
    ProductVariant v = ProductVariant.builder()
        .product(product)
        .name(name)
        .stock(stock)
        .status(SaleStatus.ON_SALE)
        .build();
    em.persist(v);
    return v;
  }

  private void flushAndClear() {
    em.flush();
    em.clear();
  }
}
