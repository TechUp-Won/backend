package com.example.WonkaoTalk.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OrderStockServiceTest {

  @Mock
  private ProductVariantRepo productVariantRepo;

  @InjectMocks
  private OrderStockService orderStockService;

  @Nested
  @DisplayName("재고 차감 검증")
  class DecreaseStocksTest {

    @Test
    @DisplayName("모든 주문 상품의 재고 차감이 성공하면 예외가 발생하지 않는다.")
    public void success_decreaseStocks() {
      //given
      OrderItem orderItem1 = mockOrderItem(10L, 2);
      OrderItem orderItem2 = mockOrderItem(20L, 1);

      when(productVariantRepo.decreaseStockAtomic(10L, 2)).thenReturn(1);
      when(productVariantRepo.decreaseStockAtomic(20L, 1)).thenReturn(1);

      //when
      orderStockService.decreaseStocks(List.of(orderItem1, orderItem2));

      //then
      verify(productVariantRepo).decreaseStockAtomic(10L, 2);
      verify(productVariantRepo).decreaseStockAtomic(20L, 1);
    }

    @Test
    @DisplayName("재고 차감 결과가 1이 아니면 PROD_STOCK_INSUFFICIENT 예외가 발생한다.")
    public void fail_decreaseStocks_stockIsNotEnough() {
      //given
      OrderItem orderItem = mockOrderItem(10L, 2);

      when(productVariantRepo.decreaseStockAtomic(10L, 2)).thenReturn(0);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderStockService.decreaseStocks(List.of(orderItem)));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROD_STOCK_INSUFFICIENT);
      verify(productVariantRepo).decreaseStockAtomic(10L, 2);
    }

    @Test
    @DisplayName("여러 주문 상품 중 하나라도 재고 차감에 실패하면 PROD_STOCK_INSUFFICIENT 예외가 발생한다.")
    public void fail_decreaseStocks_oneOfItemsStockIsNotEnough() {
      //given
      OrderItem orderItem1 = mockOrderItem(10L, 2);
      OrderItem orderItem2 = mockOrderItem(20L, 1);

      when(productVariantRepo.decreaseStockAtomic(10L, 2)).thenReturn(1);
      when(productVariantRepo.decreaseStockAtomic(20L, 1)).thenReturn(0);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderStockService.decreaseStocks(List.of(orderItem1, orderItem2)));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROD_STOCK_INSUFFICIENT);
      verify(productVariantRepo).decreaseStockAtomic(10L, 2);
      verify(productVariantRepo).decreaseStockAtomic(20L, 1);
    }
  }

  private OrderItem mockOrderItem(Long variantId, Integer quantity) {
    Order order = mock(Order.class);
    ProductVariant productVariant = mock(ProductVariant.class);
    when(productVariant.getId()).thenReturn(variantId);

    return OrderItem.createOrderItem(
        order,
        productVariant,
        "테스트 상품",
        "검정",
        10000L,
        quantity,
        "thumbnail.jpg"
    );
  }
}
