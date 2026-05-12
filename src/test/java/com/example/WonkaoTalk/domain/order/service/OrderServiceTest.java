package com.example.WonkaoTalk.domain.order.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewRequestDto.OrderItemRequest;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OrderServiceTest {

  @InjectMocks
  private OrderService orderService;

  @Test
  @DisplayName("중복 variantId가 있으면 Bad_Request 예외를 던진다.")
  public void DuplicatedVariantId() {
    //given
    OrderItemRequest item1 = mock(OrderItemRequest.class);
    OrderItemRequest item2 = mock(OrderItemRequest.class);

    //when
    when(item1.getVariantId()).thenReturn(1L);
    when(item2.getVariantId()).thenReturn(1L);

    //then
    assertThatThrownBy(() -> orderService.validateDuplicateVariant(List.of(item1, item2)))
        .isInstanceOf(BusinessException.class)
        .hasMessage(ErrorCode.BAD_REQUEST.getMessage());
  }

  @Test
  @DisplayName("중복 variantId가 없으면 예외가 발생하지 않는다.")
  public void Duplication_DoesNotThrowException_WhenVariantIdNotDuplicated() {
    //given
    OrderItemRequest item1 = mock(OrderItemRequest.class);
    OrderItemRequest item2 = mock(OrderItemRequest.class);

    //when
    when(item1.getVariantId()).thenReturn(1L);
    when(item2.getVariantId()).thenReturn(2L);

    //then
    assertThatCode(() -> orderService.validateDuplicateVariant(List.of(item1, item2)))
        .doesNotThrowAnyException();
  }


  @Test
  @DisplayName("조회하지 않은 variantId가 있으면 Not_Found 예외를 던진다.")
  public void validateVariantExist_ThrowNotFoundException_WhenVariantIdNotFound() {
    //given
    List<Long> variantIds = List.of(1L, 2L);
    ProductVariant variant = mock(ProductVariant.class);
    Map<Long, ProductVariant> variantMap = Map.of(1L, variant);

    //when Then
    assertThatThrownBy(() -> orderService.validateVariantExist(variantIds, variantMap))
        .isInstanceOf(BusinessException.class)
        .hasMessage(ErrorCode.NOT_FOUND.getMessage());
  }

  @Test
  @DisplayName("요청한 variandId가 있으면 예외가 발생하지 않는다.")
  public void validateVariantExist_doesNotThrow_whenAllVariantIdsExist() {
    //given
    List<Long> variantIds = List.of(1L, 2L);
    ProductVariant variant1 = mock(ProductVariant.class);
    ProductVariant variant2 = mock(ProductVariant.class);
    Map<Long, ProductVariant> variantMap = Map.of(1L, variant1, 2L, variant2);

    //when
    assertThatCode(() -> orderService.validateVariantExist(variantIds, variantMap))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("요청한 수량보다 재고가 많으면 예외가 발생하지 않는다.")
  public void validateVariantStock_DoesNotThrowException_WhenVariantStockEnough() {
    //given
    OrderItemRequest item1 = mock(OrderItemRequest.class);
    OrderItemRequest item2 = mock(OrderItemRequest.class);

    ProductVariant variant1 = mock(ProductVariant.class);
    ProductVariant variant2 = mock(ProductVariant.class);
    Map<Long, ProductVariant> variantMap = Map.of(1L, variant1, 2L, variant2);

    //when
    when(item1.getVariantId()).thenReturn(1L);
    when(item2.getVariantId()).thenReturn(2L);
    when(item1.getQuantity()).thenReturn(1);
    when(item2.getQuantity()).thenReturn(2);

    when(variant1.getStock()).thenReturn(10);
    when(variant2.getStock()).thenReturn(10);

    //then
    assertThatCode(() -> orderService.validateVariantStock(List.of(item1, item2), variantMap))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("요청한 수량보다 재고가 적으면 예외를 던진다.")
  public void validateVariantStock_ThrowError_WhenVariantStockIsNotEnough() {
    //given
    OrderItemRequest item1 = mock(OrderItemRequest.class);
    OrderItemRequest item2 = mock(OrderItemRequest.class);

    ProductVariant variant1 = mock(ProductVariant.class);
    ProductVariant variant2 = mock(ProductVariant.class);
    Map<Long, ProductVariant> variantMap = Map.of(1L, variant1, 2L, variant2);

    //when
    when(item1.getVariantId()).thenReturn(1L);
    when(item2.getVariantId()).thenReturn(2L);
    when(item1.getQuantity()).thenReturn(1);
    when(item2.getQuantity()).thenReturn(2);

    when(variant1.getStock()).thenReturn(10);
    when(variant2.getStock()).thenReturn(1);

    //then
    assertThatThrownBy(() -> orderService.validateVariantStock(List.of(item1, item2), variantMap))
        .isInstanceOf(BusinessException.class)
        .hasMessage(ErrorCode.PROD_STOCK_INSUFFICIENT.getMessage());
  }


}
