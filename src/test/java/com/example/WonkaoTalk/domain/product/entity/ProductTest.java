package com.example.WonkaoTalk.domain.product.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductTest {

  // ── update() ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("null 필드는 기존 값을 유지한다")
  void update_nullFields_preservesExistingValues() {
    // given
    Product product = Product.builder()
        .name("기존상품명")
        .price(10000)
        .discountRate(0)
        .discountedPrice(10000)
        .status(SaleStatus.ON_SALE)
        .store(mock(com.example.WonkaoTalk.domain.store.entity.Store.class))
        .category(mock(Category.class))
        .build();

    // when
    product.update(null, null, null, null, null, null);

    // then
    assertThat(product.getName()).isEqualTo("기존상품명");
    assertThat(product.getPrice()).isEqualTo(10000);
    assertThat(product.getDiscountedPrice()).isEqualTo(10000);
    assertThat(product.getStatus()).isEqualTo(SaleStatus.ON_SALE);
  }

  @Test
  @DisplayName("이름 전달 시 이름이 변경된다")
  void update_name_changesName() {
    // given
    Product product = Product.builder()
        .name("기존상품명")
        .price(10000)
        .discountRate(0)
        .discountedPrice(10000)
        .status(SaleStatus.ON_SALE)
        .store(mock(com.example.WonkaoTalk.domain.store.entity.Store.class))
        .category(mock(Category.class))
        .build();

    // when
    product.update("새상품명", null, null, null, null, null);

    // then
    assertThat(product.getName()).isEqualTo("새상품명");
  }

  @Test
  @DisplayName("price만 변경 시 기존 discountRate로 discountedPrice를 재계산한다")
  void update_priceOnly_recalculatesDiscountedPriceWithExistingDiscountRate() {
    // given: discountRate=20%, price=10000 → discountedPrice=8000
    Product product = Product.builder()
        .name("상품")
        .price(10000)
        .discountRate(20)
        .discountedPrice(8000)
        .status(SaleStatus.ON_SALE)
        .store(mock(com.example.WonkaoTalk.domain.store.entity.Store.class))
        .category(mock(Category.class))
        .build();

    // when: price를 20000으로 변경
    product.update(null, null, null, 20000, null, null);

    // then: discountedPrice = 20000 * (1 - 20/100.0) = 16000
    assertThat(product.getPrice()).isEqualTo(20000);
    assertThat(product.getDiscountedPrice()).isEqualTo(16000);
  }

  @Test
  @DisplayName("discountRate만 변경 시 기존 price로 discountedPrice를 재계산한다")
  void update_discountRateOnly_recalculatesDiscountedPriceWithExistingPrice() {
    // given: discountRate=0%, price=10000 → discountedPrice=10000
    Product product = Product.builder()
        .name("상품")
        .price(10000)
        .discountRate(0)
        .discountedPrice(10000)
        .status(SaleStatus.ON_SALE)
        .store(mock(com.example.WonkaoTalk.domain.store.entity.Store.class))
        .category(mock(Category.class))
        .build();

    // when: discountRate를 30으로 변경
    product.update(null, null, null, null, 30, null);

    // then: discountedPrice = 10000 * (1 - 30/100.0) = 7000
    assertThat(product.getDiscountRate()).isEqualTo(30);
    assertThat(product.getDiscountedPrice()).isEqualTo(7000);
  }

  @Test
  @DisplayName("discountRate가 null인 상품에서 price 변경 시 할인율 0으로 재계산한다")
  void update_priceOnly_whenDiscountRateIsNull_usesZeroRate() {
    // given: discountRate=null, price=10000 → discountedPrice=10000
    Product product = Product.builder()
        .name("상품")
        .price(10000)
        .discountRate(null)
        .discountedPrice(10000)
        .status(SaleStatus.ON_SALE)
        .store(mock(com.example.WonkaoTalk.domain.store.entity.Store.class))
        .category(mock(Category.class))
        .build();

    // when
    product.update(null, null, null, 15000, null, null);

    // then: discountedPrice = 15000 * (1 - 0/100.0) = 15000
    assertThat(product.getDiscountedPrice()).isEqualTo(15000);
  }

  @Test
  @DisplayName("status 전달 시 상태가 변경된다")
  void update_status_changesStatus() {
    // given
    Product product = Product.builder()
        .name("상품")
        .price(10000)
        .discountRate(0)
        .discountedPrice(10000)
        .status(SaleStatus.ON_SALE)
        .store(mock(com.example.WonkaoTalk.domain.store.entity.Store.class))
        .category(mock(Category.class))
        .build();

    // when
    product.update(null, null, null, null, null, SaleStatus.STOP_SALE);

    // then
    assertThat(product.getStatus()).isEqualTo(SaleStatus.STOP_SALE);
  }

  // ── softDelete() ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("softDelete 호출 시 deletedAt이 설정되고 상태가 STOP_SALE로 변경된다")
  void softDelete_setsDeletedAtAndStopSaleStatus() {
    // given
    Product product = Product.builder()
        .name("상품")
        .price(10000)
        .discountRate(0)
        .discountedPrice(10000)
        .status(SaleStatus.ON_SALE)
        .store(mock(com.example.WonkaoTalk.domain.store.entity.Store.class))
        .category(mock(Category.class))
        .build();

    // when
    product.softDelete();

    // then
    assertThat(product.getDeletedAt()).isNotNull();
    assertThat(product.getStatus()).isEqualTo(SaleStatus.STOP_SALE);
  }

  // ── isOnSale() ───────────────────────────────────────────────────────────────

  @Test
  @DisplayName("deletedAt이 null이고 ON_SALE이면 true를 반환한다")
  void isOnSale_deletedAtNullAndOnSale_returnsTrue() {
    // given
    Product product = Product.builder()
        .name("상품")
        .price(10000)
        .discountRate(0)
        .discountedPrice(10000)
        .status(SaleStatus.ON_SALE)
        .store(mock(com.example.WonkaoTalk.domain.store.entity.Store.class))
        .category(mock(Category.class))
        .build();

    // when & then
    assertThat(product.isOnSale()).isTrue();
  }

  @Test
  @DisplayName("상태가 STOP_SALE이면 false를 반환한다")
  void isOnSale_stopSaleStatus_returnsFalse() {
    // given
    Product product = Product.builder()
        .name("상품")
        .price(10000)
        .discountRate(0)
        .discountedPrice(10000)
        .status(SaleStatus.STOP_SALE)
        .store(mock(com.example.WonkaoTalk.domain.store.entity.Store.class))
        .category(mock(Category.class))
        .build();

    // when & then
    assertThat(product.isOnSale()).isFalse();
  }

  @Test
  @DisplayName("softDelete된 상품은 isOnSale이 false를 반환한다")
  void isOnSale_softDeleted_returnsFalse() {
    // given
    Product product = Product.builder()
        .name("상품")
        .price(10000)
        .discountRate(0)
        .discountedPrice(10000)
        .status(SaleStatus.ON_SALE)
        .store(mock(com.example.WonkaoTalk.domain.store.entity.Store.class))
        .category(mock(Category.class))
        .build();
    product.softDelete();

    // when & then
    assertThat(product.isOnSale()).isFalse();
  }
}
