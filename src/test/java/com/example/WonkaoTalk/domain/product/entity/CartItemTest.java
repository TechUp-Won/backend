package com.example.WonkaoTalk.domain.product.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CartItemTest {

  @Test
  @DisplayName("addQuantity 호출 시 수량이 지정한 값만큼 증가한다")
  void addQuantity_increasesQuantityByAmount() {
    // given
    CartItem item = CartItem.builder()
        .cart(mock(Cart.class))
        .productVariant(mock(ProductVariant.class))
        .quantity(3)
        .build();

    // when
    item.addQuantity(2);

    // then
    assertThat(item.getQuantity()).isEqualTo(5);
  }

  @Test
  @DisplayName("updateQuantity 호출 시 수량이 전달한 값으로 교체된다")
  void updateQuantity_replacesQuantityWithGivenValue() {
    // given
    CartItem item = CartItem.builder()
        .cart(mock(Cart.class))
        .productVariant(mock(ProductVariant.class))
        .quantity(3)
        .build();

    // when
    item.updateQuantity(10);

    // then
    assertThat(item.getQuantity()).isEqualTo(10);
  }

  @Test
  @DisplayName("updateVariant 호출 시 ProductVariant가 전달한 값으로 교체된다")
  void updateVariant_replacesProductVariant() {
    // given
    ProductVariant oldVariant = mock(ProductVariant.class);
    ProductVariant newVariant = ProductVariant.builder()
        .product(mock(Product.class))
        .stock(5)
        .status(SaleStatus.ON_SALE)
        .build();
    CartItem item = CartItem.builder()
        .cart(mock(Cart.class))
        .productVariant(oldVariant)
        .quantity(1)
        .build();

    // when
    item.updateVariant(newVariant);

    // then
    assertThat(item.getProductVariant()).isSameAs(newVariant);
  }
}
