package com.example.WonkaoTalk.domain.product.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductVariantTest {

  // ── adjustStock() ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("재고 증가 시 stock이 정상적으로 늘어난다")
  void adjustStock_increase_addsToStock() {
    // given
    Product product = buildProduct(SaleStatus.ON_SALE);
    ProductVariant variant = buildVariant(product, 10, SaleStatus.ON_SALE);

    // when
    variant.adjustStock(5);

    // then
    assertThat(variant.getStock()).isEqualTo(15);
    assertThat(variant.getStatus()).isEqualTo(SaleStatus.ON_SALE);
  }

  @Test
  @DisplayName("재고 감소 시 stock이 정상적으로 줄어든다")
  void adjustStock_decrease_subtractsFromStock() {
    // given
    Product product = buildProduct(SaleStatus.ON_SALE);
    ProductVariant variant = buildVariant(product, 10, SaleStatus.ON_SALE);

    // when
    variant.adjustStock(-3);

    // then
    assertThat(variant.getStock()).isEqualTo(7);
    assertThat(variant.getStatus()).isEqualTo(SaleStatus.ON_SALE);
  }

  @Test
  @DisplayName("재고가 음수가 되면 IllegalArgumentException이 발생한다")
  void adjustStock_negativeResult_throwsIllegalArgumentException() {
    // given
    Product product = buildProduct(SaleStatus.ON_SALE);
    ProductVariant variant = buildVariant(product, 3, SaleStatus.ON_SALE);

    // when & then
    assertThatThrownBy(() -> variant.adjustStock(-5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("재고는 0 미만이 될 수 없습니다.");
  }

  // ── isSellable() ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("deletedAt이 null이고 상품이 ON_SALE이고 옵션이 ON_SALE이면 true를 반환한다")
  void isSellable_allConditionsMet_returnsTrue() {
    // given
    Product product = buildProduct(SaleStatus.ON_SALE);
    ProductVariant variant = buildVariant(product, 10, SaleStatus.ON_SALE);

    // when & then
    assertThat(variant.isSellable()).isTrue();
  }

  @Test
  @DisplayName("옵션이 삭제(deletedAt 설정)되어 있으면 false를 반환한다")
  void isSellable_deletedVariant_returnsFalse() {
    // given
    Product product = buildProduct(SaleStatus.ON_SALE);
    ProductVariant variant = ProductVariant.builder()
        .product(product)
        .stock(10)
        .status(SaleStatus.ON_SALE)
        .deletedAt(java.time.LocalDateTime.now())
        .build();

    // when & then
    assertThat(variant.isSellable()).isFalse();
  }

  @Test
  @DisplayName("상품이 판매 중이 아니면 false를 반환한다")
  void isSellable_productNotOnSale_returnsFalse() {
    // given
    Product product = buildProduct(SaleStatus.STOP_SALE);
    ProductVariant variant = buildVariant(product, 10, SaleStatus.ON_SALE);

    // when & then
    assertThat(variant.isSellable()).isFalse();
  }

  @Test
  @DisplayName("옵션 상태가 ON_SALE이 아니면 false를 반환한다")
  void isSellable_variantNotOnSale_returnsFalse() {
    // given
    Product product = buildProduct(SaleStatus.ON_SALE);
    ProductVariant variant = buildVariant(product, 0, SaleStatus.OUT_OF_STOCK);

    // when & then
    assertThat(variant.isSellable()).isFalse();
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

  private Product buildProduct(SaleStatus status) {
    return Product.builder()
        .name("테스트상품")
        .price(10000)
        .discountRate(0)
        .discountedPrice(10000)
        .status(status)
        .store(mock(com.example.WonkaoTalk.domain.store.entity.Store.class))
        .category(mock(Category.class))
        .build();
  }

  private ProductVariant buildVariant(Product product, int stock, SaleStatus status) {
    return ProductVariant.builder()
        .product(product)
        .stock(stock)
        .status(status)
        .build();
  }
}
