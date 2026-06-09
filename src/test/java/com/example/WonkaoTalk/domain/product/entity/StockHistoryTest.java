package com.example.WonkaoTalk.domain.product.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.WonkaoTalk.domain.product.enums.StockChangeReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StockHistoryTest {

  @Test
  @DisplayName("차감 시 stockAfter는 stockBefore + changeAmount(음수)이다")
  void of_decrease_stockAfterCalculatedCorrectly() {
    ProductVariant variant = mock(ProductVariant.class);

    StockHistory history = StockHistory.of(variant, null, -3, 10, StockChangeReason.SALE);

    assertThat(history.getChangeAmount()).isEqualTo(-3);
    assertThat(history.getStockBefore()).isEqualTo(10);
    assertThat(history.getStockAfter()).isEqualTo(7);
    assertThat(history.getReason()).isEqualTo(StockChangeReason.SALE);
  }

  @Test
  @DisplayName("입고 시 stockAfter는 stockBefore + changeAmount(양수)이다")
  void of_increase_stockAfterCalculatedCorrectly() {
    ProductVariant variant = mock(ProductVariant.class);

    StockHistory history = StockHistory.of(variant, null, 10, 0, StockChangeReason.INITIAL_STOCK);

    assertThat(history.getChangeAmount()).isEqualTo(10);
    assertThat(history.getStockBefore()).isEqualTo(0);
    assertThat(history.getStockAfter()).isEqualTo(10);
    assertThat(history.getReason()).isEqualTo(StockChangeReason.INITIAL_STOCK);
  }
}
