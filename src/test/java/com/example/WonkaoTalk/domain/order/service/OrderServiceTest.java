package com.example.WonkaoTalk.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequest;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequest.DeliveryRequestDto;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderItemDto;
import com.example.WonkaoTalk.domain.order.entity.Delivery;
import com.example.WonkaoTalk.domain.order.entity.DeliveryStatus;
import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import com.example.WonkaoTalk.domain.order.repo.DeliveryRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderRepo;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

  @Mock
  private UserRepo userRepo;

  @Mock
  private ProductVariantRepo productVariantRepo;

  @Mock
  private OrderRepo orderRepo;

  @Mock
  private OrderItemRepo orderItemRepo;

  @Mock
  private DeliveryRepo deliveryRepo;

  @Mock
  private OrderStockService orderStockService;

  @InjectMocks
  private OrderService orderService;

  @Nested
  @DisplayName("중복 variantId 검증")
  class ValidateDuplicateVariantTest {

    @Test
    @DisplayName("중복 variantId가 있으면 BAD_REQUEST 예외가 발생한다.")
    public void fail_validateDuplicateVariant_duplicatedVariantId() {
      //given
      OrderItemDto item1 = new OrderItemDto(1L, 1);
      OrderItemDto item2 = new OrderItemDto(1L, 2);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.validateDuplicateVariant(List.of(item1, item2)));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("중복 variantId가 없으면 예외가 발생하지 않는다.")
    public void success_validateDuplicateVariant() {
      //given
      OrderItemDto item1 = new OrderItemDto(1L, 1);
      OrderItemDto item2 = new OrderItemDto(2L, 2);

      //when then
      assertThatCode(() -> orderService.validateDuplicateVariant(List.of(item1, item2)))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("variant 존재 검증")
  class ValidateVariantExistTest {

    @Test
    @DisplayName("조회하지 않은 variantId가 있으면 NOT_FOUND 예외가 발생한다.")
    public void fail_validateVariantExist_variantIdNotFound() {
      //given
      List<Long> variantIds = List.of(1L, 2L);
      ProductVariant variant = mock(ProductVariant.class);
      Map<Long, ProductVariant> variantMap = Map.of(1L, variant);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.validateVariantExist(variantIds, variantMap));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("요청한 variantId가 모두 있으면 예외가 발생하지 않는다.")
    public void success_validateVariantExist() {
      //given
      List<Long> variantIds = List.of(1L, 2L);
      ProductVariant variant1 = mock(ProductVariant.class);
      ProductVariant variant2 = mock(ProductVariant.class);
      Map<Long, ProductVariant> variantMap = Map.of(1L, variant1, 2L, variant2);

      //when then
      assertThatCode(() -> orderService.validateVariantExist(variantIds, variantMap))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("variant 재고 검증")
  class ValidateVariantStockTest {

    @Test
    @DisplayName("요청한 수량보다 재고가 많으면 예외가 발생하지 않는다.")
    public void success_validateVariantStock() {
      //given
      OrderItemDto item1 = new OrderItemDto(1L, 1);
      OrderItemDto item2 = new OrderItemDto(2L, 2);

      ProductVariant variant1 = mock(ProductVariant.class);
      ProductVariant variant2 = mock(ProductVariant.class);
      Map<Long, ProductVariant> variantMap = Map.of(1L, variant1, 2L, variant2);

      when(variant1.getStock()).thenReturn(10);
      when(variant2.getStock()).thenReturn(10);

      //when then
      assertThatCode(() -> orderService.validateVariantStock(List.of(item1, item2), variantMap))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("요청한 수량보다 재고가 적으면 PROD_STOCK_INSUFFICIENT 예외가 발생한다.")
    public void fail_validateVariantStock_stockIsNotEnough() {
      //given
      OrderItemDto item1 = new OrderItemDto(1L, 1);
      OrderItemDto item2 = new OrderItemDto(2L, 2);

      ProductVariant variant1 = mock(ProductVariant.class);
      ProductVariant variant2 = mock(ProductVariant.class);
      Map<Long, ProductVariant> variantMap = Map.of(1L, variant1, 2L, variant2);

      when(variant1.getStock()).thenReturn(10);
      when(variant2.getStock()).thenReturn(1);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.validateVariantStock(List.of(item1, item2), variantMap));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROD_STOCK_INSUFFICIENT);
    }
  }

  @Nested
  @DisplayName("주문 생성 검증")
  class CreateOrderTest {

    @Test
    @DisplayName("주문 생성 시 요청 배송 정보를 Delivery로 저장한다.")
    public void success_createOrder_saveDeliveryInfo() {
      //given
      OrderCreateRequest request = mockOrderCreateRequest();
      mockCreateOrderDependencies(request);

      //when
      OrderCreateResponse response = orderService.createOrder(1L, request);

      //then
      ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
      verify(deliveryRepo).save(deliveryCaptor.capture());

      Delivery savedDelivery = deliveryCaptor.getValue();
      assertThat(savedDelivery.getRecipientName()).isEqualTo("홍길동");
      assertThat(savedDelivery.getRecipientPhone()).isEqualTo("010-1234-5678");
      assertThat(savedDelivery.getZipcode()).isEqualTo("06234");
      assertThat(savedDelivery.getAddress()).isEqualTo("서울특별시 강남구 테헤란로");
      assertThat(savedDelivery.getAddressDetail()).isEqualTo("101동 1001호");
      assertThat(savedDelivery.getMemo()).isEqualTo("문 앞에 놔주세요");
      assertThat(savedDelivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.PREPARING);
      assertThat(savedDelivery.getOrder()).isNotNull();

      assertThat(response.orderInfo().orderTitle()).isEqualTo("테스트 상품");
      assertThat(response.orderInfo().originalAmount()).isEqualTo(24000L);
      assertThat(response.orderInfo().discountAmount()).isEqualTo(4000L);
      assertThat(response.orderInfo().finalAmount()).isEqualTo(20000L);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("주문 생성 시 재고 차감 요청을 한다.")
    public void success_createOrder_decreaseStocks() {
      //given
      OrderCreateRequest request = mockOrderCreateRequest();
      mockCreateOrderDependencies(request);

      //when
      orderService.createOrder(1L, request);

      //then
      ArgumentCaptor<List<OrderItem>> orderItemsCaptor = ArgumentCaptor.forClass(List.class);
      verify(orderStockService).decreaseStocks(orderItemsCaptor.capture());
      assertThat(orderItemsCaptor.getValue()).hasSize(1);
      assertThat(orderItemsCaptor.getValue().getFirst().getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("유저가 존재하지 않으면 NOT_FOUND 예외가 발생하고 주문을 저장하지 않는다.")
    public void fail_createOrder_userNotFound() {
      //given
      OrderCreateRequest request = mockOrderCreateRequest();
      when(userRepo.findById(1L)).thenReturn(Optional.empty());

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.createOrder(1L, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      verify(orderRepo, never()).save(any(Order.class));
      verify(orderStockService, never()).decreaseStocks(anyList());
    }
  }

  @Nested
  @DisplayName("주문 삭제 검증")
  class DeleteOrderTest {

    @Test
    @DisplayName("주문 삭제 시 Repository delete를 호출한다.")
    public void success_deleteOrder() {
      //given
      Long userId = 1L;
      Long orderId = 10L;
      Order order = mock(Order.class);

      when(orderRepo.findByUserIdAndOrderId(userId, orderId)).thenReturn(Optional.of(order));

      //when
      orderService.deleteOrder(userId, orderId);

      //then
      verify(orderRepo).delete(order);
    }

    @Test
    @DisplayName("주문이 존재하지 않으면 ORDER_NOT_FOUND 예외가 발생한다.")
    public void fail_deleteOrder_orderNotFound() {
      //given
      Long userId = 1L;
      Long orderId = 10L;

      when(orderRepo.findByUserIdAndOrderId(userId, orderId)).thenReturn(Optional.empty());

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.deleteOrder(userId, orderId));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
      verify(orderRepo, never()).delete(any(Order.class));
    }
  }

  private OrderCreateRequest mockOrderCreateRequest() {
    return new OrderCreateRequest(
        List.of(new OrderItemDto(10L, 2)),
        new DeliveryRequestDto(
            "홍길동",
            "010-1234-5678",
            "06234",
            "서울특별시 강남구 테헤란로",
            "101동 1001호",
            "문 앞에 놔주세요"
        ),
        0L
    );
  }

  private void mockCreateOrderDependencies(OrderCreateRequest request) {
    User user = mock(User.class);
    Product product = mock(Product.class);
    ProductVariant variant = mock(ProductVariant.class);

    when(userRepo.findById(1L)).thenReturn(Optional.of(user));
    when(productVariantRepo.findAllById(List.of(10L))).thenReturn(List.of(variant));
    when(orderRepo.existsByOrderNumber(any())).thenReturn(false);
    when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(orderItemRepo.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

    lenient().when(variant.getId()).thenReturn(10L);
    lenient().when(variant.getStock()).thenReturn(10);
    lenient().when(variant.getStatus()).thenReturn(SaleStatus.ON_SALE);
    lenient().when(variant.getProduct()).thenReturn(product);
    lenient().when(variant.getName()).thenReturn("검정");

    lenient().when(product.getId()).thenReturn(100L);
    lenient().when(product.getName()).thenReturn("테스트 상품");
    lenient().when(product.getThumbnail()).thenReturn("thumbnail.jpg");
    lenient().when(product.getPrice()).thenReturn(12000);
    lenient().when(product.getDiscountedPrice()).thenReturn(10000);
  }
}
