package com.example.WonkaoTalk.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequest;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequest.DeliveryRequestDto;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderDetailResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderItemDto;
import com.example.WonkaoTalk.domain.order.dto.OrderListResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewRequest;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewResponse;
import com.example.WonkaoTalk.domain.order.entity.Delivery;
import com.example.WonkaoTalk.domain.order.entity.DeliveryStatus;
import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import com.example.WonkaoTalk.domain.order.entity.OrderStatus;
import com.example.WonkaoTalk.domain.order.event.OrderStockRestoreRequestEvent;
import com.example.WonkaoTalk.domain.order.repo.DeliveryRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderRepo;
import com.example.WonkaoTalk.domain.payment.entity.Payment;
import com.example.WonkaoTalk.domain.payment.entity.PaymentStatus;
import com.example.WonkaoTalk.domain.payment.entity.PgProvider;
import com.example.WonkaoTalk.domain.payment.repo.PaymentRepo;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import java.time.LocalDateTime;
import java.util.Collections;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
  private PaymentRepo paymentRepo;

  @Mock
  private OrderStockService orderStockService;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private OrderService orderService;

  @Nested
  @DisplayName("주문 미리보기 검증")
  class PreviewOrderTest {

    @Test
    @DisplayName("주문 미리보기 성공")
    public void success_previewOrder() {
      //given
      OrderPreviewRequest request = mockOrderPreviewRequest();
      ProductVariant productVariant = mockProductVariant();
      when(productVariantRepo.findAllById(List.of(10L))).thenReturn(List.of(productVariant));
      when(productVariant.getId()).thenReturn(10L);
      when(productVariant.getStatus()).thenReturn(SaleStatus.ON_SALE);
      when(productVariant.getStock()).thenReturn(2);

      Product product = mock(Product.class);
      when(productVariant.getProduct()).thenReturn(product);
      when(product.getId()).thenReturn(100L);
      when(product.getName()).thenReturn("테스트 상품");
      when(product.getThumbnail()).thenReturn("thumbnail.jpg");
      when(product.getPrice()).thenReturn(12000);
      when(product.getDiscountedPrice()).thenReturn(10000);

      //when
      OrderPreviewResponse response = orderService.previewOrder(request);

      //then
      assertThat(response.items()).hasSize(1);
      assertThat(response.items().getFirst().productId()).isEqualTo(100L);
      assertThat(response.items().getFirst().variantId()).isEqualTo(10L);
      assertThat(response.items().getFirst().productName()).isEqualTo("테스트 상품");
      assertThat(response.items().getFirst().price()).isEqualTo(12000L);
      assertThat(response.items().getFirst().discountedPrice()).isEqualTo(10000L);
      assertThat(response.items().getFirst().quantity()).isEqualTo(2);
      assertThat(response.items().getFirst().itemOriginalAmount()).isEqualTo(24000L);
      assertThat(response.items().getFirst().itemDiscountAmount()).isEqualTo(4000L);
      assertThat(response.items().getFirst().itemFinalAmount()).isEqualTo(20000L);

      assertThat(response.summary().originalAmount()).isEqualTo(24000L);
      assertThat(response.summary().discountAmount()).isEqualTo(4000L);
      assertThat(response.summary().finalAmount()).isEqualTo(20000L);

    }

    @Test
    @DisplayName("중복 variantId면 BAD_REQUEST를 반환한다.")
    public void fail_previewOrder_duplicatedVariantId() {
      //given
      OrderPreviewRequest request = new OrderPreviewRequest(
          List.of(
              new OrderItemDto(10L, 2),
              new OrderItemDto(10L, 2))
      );

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.previewOrder(request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("존재하지 않는 variantId면 NOT_FOUND 반환")
    public void fail_previewOrder_variantNotFound() {
      //given
      OrderPreviewRequest request = mockOrderPreviewRequest();
      ProductVariant variant = mock(ProductVariant.class);
      when(productVariantRepo.findAllById(List.of(10L))).thenReturn(Collections.emptyList());
      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.previewOrder(request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("ON_SALE이 아니면 PROD_VARIANT_UNAVAILABLE를 반환한다.")
    public void fail_previewOrder_variantUnavailable() {
      //given
      OrderPreviewRequest request = mockOrderPreviewRequest();
      ProductVariant productVariant = mock(ProductVariant.class);
      when(productVariantRepo.findAllById(List.of(10L))).thenReturn(List.of(productVariant));
      when(productVariant.getId()).thenReturn(10L);
      when(productVariant.getStatus()).thenReturn(SaleStatus.STOP_SALE);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.previewOrder(request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROD_VARIANT_UNAVAILABLE);
    }

    @Test
    @DisplayName("재고 부족이면 PROD_STOCK_INSUFFICIENT를 반환한다.")
    public void fail_previewOrder_stockInsufficient() {
      //given
      OrderPreviewRequest request = mockOrderPreviewRequest();
      ProductVariant variant = mock(ProductVariant.class);
      when(productVariantRepo.findAllById(List.of(10L))).thenReturn(List.of(variant));
      when(variant.getId()).thenReturn(10L);
      when(variant.getStatus()).thenReturn(SaleStatus.ON_SALE);
      when(variant.getStock()).thenReturn(1);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.previewOrder(request));

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

    @Test
    @DisplayName("중복 variantId면 BAD_REQUEST")
    public void fail_createOrder_duplicatedVariantId() {
      //given
      OrderCreateRequest request = new OrderCreateRequest(
          List.of(new OrderItemDto(10L, 2), new OrderItemDto(10L, 2)),
          mockDeliveryRequest(),
          0L
      );
      User user = mock(User.class);
      when(userRepo.findById(1L)).thenReturn(Optional.of(user));

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.createOrder(1L, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("존재하지 않는 variantId면 NOT_FOUND")
    public void fail_createOrder_variantNotFound() {
      //given
      OrderCreateRequest request = mockOrderCreateRequest();
      User user = mock(User.class);
      when(userRepo.findById(1L)).thenReturn(Optional.of(user));
      // 또는 when(productVariantRepo.findAllById(anyList())).thenReturn(Collection.EmptyList());
      when(productVariantRepo.findAllById(List.of(10L))).thenReturn(List.of());
      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.createOrder(1L, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("ON_SALE이 아니면 PROD_VARIANT_UNAVAILABLE")
    public void fail_createOrder_variantUnavailable() {
      //given
      OrderCreateRequest request = mockOrderCreateRequest();
      User user = mock(User.class);
      when(userRepo.findById(1L)).thenReturn(Optional.of(user));
      ProductVariant productVariant = mock(ProductVariant.class);
      when(productVariant.getId()).thenReturn(10L);
      when(productVariant.getStatus()).thenReturn(SaleStatus.STOP_SALE);
      when(productVariantRepo.findAllById(List.of(10L))).thenReturn(List.of(productVariant));

      // variantLId 목록으로
      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.createOrder(1L, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROD_VARIANT_UNAVAILABLE);
    }

    @Test
    @DisplayName("요청 수량보다 재고가 부족하면 PROD_STOCK_INSUFFICIENT 예외가 발생한다.")
    public void fail_createOrder_stockInsufficient() {
      //given
      OrderCreateRequest request = mockOrderCreateRequest();
      User user = mock(User.class);
      when(userRepo.findById(1L)).thenReturn(Optional.of(user));
      ProductVariant productVariant = mock(ProductVariant.class);
      when(productVariant.getId()).thenReturn(10L);
      when(productVariant.getStatus()).thenReturn(SaleStatus.ON_SALE);
      when(productVariantRepo.findAllById(List.of(10L))).thenReturn(List.of(productVariant));
      when(productVariant.getStock()).thenReturn(1);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.createOrder(1L, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROD_STOCK_INSUFFICIENT);
    }

    @Test
    @DisplayName("주문번호가 중복되면 재시도 후 주문을 생성한다.")
    public void success_createOrder_retryDuplicatedOrderNumber() {
      //given
      OrderCreateRequest request = mockOrderCreateRequest();
      User user = mock(User.class);
      when(userRepo.findById(1L)).thenReturn(Optional.of(user));
      ProductVariant productVariant = mock(ProductVariant.class);
      when(productVariant.getId()).thenReturn(10L);
      when(productVariant.getStatus()).thenReturn(SaleStatus.ON_SALE);
      when(productVariantRepo.findAllById(List.of(10L))).thenReturn(List.of(productVariant));
      when(productVariant.getStock()).thenReturn(2);
      when(orderRepo.existsByOrderNumber(any()))
          .thenReturn(true)
          .thenReturn(false);
      when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(orderItemRepo.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

      Product product = mock(Product.class);
      when(productVariant.getProduct()).thenReturn(product);
      when(product.getId()).thenReturn(100L);
      when(product.getName()).thenReturn("테스트 상품");
      when(product.getThumbnail()).thenReturn("thumbnail.jpg");
      when(product.getPrice()).thenReturn(12000);
      when(product.getDiscountedPrice()).thenReturn(10000);
      //when
      OrderCreateResponse response = orderService.createOrder(1L, request);

      //then
      verify(orderRepo, times(2)).existsByOrderNumber(any());

    }

    @Test
    @DisplayName("OrderNumber가 5회 모두 중복이면 SERVER_ERROR를 발생한다.")
    public void fail_createOrder_orderNumberDuplicatedFiveTimes() {
      //given
      OrderCreateRequest request = mockOrderCreateRequest();
      User user = mock(User.class);
      when(userRepo.findById(1L)).thenReturn(Optional.of(user));
      ProductVariant productVariant = mock(ProductVariant.class);
      when(productVariant.getId()).thenReturn(10L);
      when(productVariant.getStatus()).thenReturn(SaleStatus.ON_SALE);
      when(productVariantRepo.findAllById(List.of(10L))).thenReturn(List.of(productVariant));
      when(productVariant.getStock()).thenReturn(2);
      when(orderRepo.existsByOrderNumber(any()))
          .thenReturn(true)
          .thenReturn(true)
          .thenReturn(true)
          .thenReturn(true)
          .thenReturn(true);

      Product product = mock(Product.class);
      when(productVariant.getProduct()).thenReturn(product);
      when(product.getId()).thenReturn(100L);
      when(product.getName()).thenReturn("테스트 상품");
      when(product.getThumbnail()).thenReturn("thumbnail.jpg");
      when(product.getPrice()).thenReturn(12000);
      when(product.getDiscountedPrice()).thenReturn(10000);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.createOrder(1L, request));
      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVER_ERROR);
    }

    @Test
    @DisplayName("상품 2개 이상이면 주문 명에 상품명 외 N건을 반환한다.")
    public void success_createOrder_multipleItems_orderTitle() {
      //given
      OrderCreateRequest request = new OrderCreateRequest(
          List.of(new OrderItemDto(10L, 2), new OrderItemDto(11L, 2)),
          mockDeliveryRequest(),
          0L
      );
      User user = mock(User.class);
      when(userRepo.findById(1L)).thenReturn(Optional.of(user));
      ProductVariant productVariant1 = mock(ProductVariant.class);
      ProductVariant productVariant2 = mock(ProductVariant.class);

      when(productVariant1.getId()).thenReturn(10L);
      when(productVariant1.getStatus()).thenReturn(SaleStatus.ON_SALE);
      when(productVariant1.getStock()).thenReturn(2);
      when(productVariant1.getName()).thenReturn("검정");

      when(productVariant2.getId()).thenReturn(11L);
      when(productVariant2.getStatus()).thenReturn(SaleStatus.ON_SALE);
      when(productVariant2.getStock()).thenReturn(2);
      when(productVariant2.getName()).thenReturn("흰색");

      when(productVariantRepo.findAllById(List.of(10L, 11L))).thenReturn(
          List.of(productVariant1, productVariant2));
      when(orderRepo.existsByOrderNumber(any()))
          .thenReturn(false);
      when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(orderItemRepo.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

      Product product1 = mock(Product.class);

      when(productVariant1.getProduct()).thenReturn(product1);
      when(product1.getId()).thenReturn(100L);
      when(product1.getName()).thenReturn("테스트 상품1");
      when(product1.getThumbnail()).thenReturn("thumbnail.jpg");
      when(product1.getPrice()).thenReturn(12000);
      when(product1.getDiscountedPrice()).thenReturn(10000);

      Product product2 = mock(Product.class);
      when(productVariant2.getProduct()).thenReturn(product2);
      when(product2.getId()).thenReturn(101L);
      when(product2.getName()).thenReturn("테스트 상품2");
      when(product2.getThumbnail()).thenReturn("thumbnail.jpg");
      when(product2.getPrice()).thenReturn(12000);
      when(product2.getDiscountedPrice()).thenReturn(10000);

      //when
      OrderCreateResponse response = orderService.createOrder(1L, request);

      //then
      assertThat(response.orderInfo().orderTitle()).isEqualTo("테스트 상품1 외 1건");
    }
  }

  @Nested
  @DisplayName("주문 목록 조회 검증")
  class GetOrdersTest {

    @Test
    @DisplayName("주문 목록과 PageInfo를 반환한다.")
    public void success_getOrders() {
      //given
      Order order = mockOrder();
      Pageable pageable = PageRequest.of(0, 10);
      Page<Order> orderPage = new PageImpl<>(
          List.of(order),
          pageable,
          1
      );
      when(orderRepo.findByUserId(1L, pageable)).thenReturn((orderPage));

      //when
      OrderListResponse response = orderService.getOrders(1L, pageable);

      //then
      assertThat(response).isNotNull();
      assertThat(response.orders()).hasSize(1);

      assertThat(response.pageInfo()).isNotNull();
      assertThat(response.pageInfo().page()).isEqualTo(0);
      assertThat(response.pageInfo().size()).isEqualTo(10);
      assertThat(response.pageInfo().totalElements()).isEqualTo(1);
      assertThat(response.pageInfo().totalPages()).isEqualTo(1);

      assertThat(response.orders().get(0).orderId()).isEqualTo(order.getOrderId());
    }
  }

  @Nested
  @DisplayName("주문 상세 조회 검증")
  class GetOrderDetailTest {

    @Test
    @DisplayName("주문, 주문 상품, 결제 목록을 반환한다.")
    public void success_getOrderDetail() {
      //given
      Order order = mockOrder();
      List<OrderItem> orderItems = List.of(
          mockOrderItem(order, 1L, "포카칩"),
          mockOrderItem(order, 2L, "썬칩"));
      Payment payment = mockPayment();
      when(orderRepo.findByUserIdAndOrderId(1L, order.getOrderId())).thenReturn(Optional.of(order));
      when(orderItemRepo.findByOrder(order)).thenReturn(orderItems);
      when(paymentRepo.findByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId())).thenReturn(
          List.of(payment));

      //when
      OrderDetailResponse response = orderService.getOrderDetail(1L, order.getOrderId());

      //then
      assertThat(response).isNotNull();
      assertThat(response.orderInfo()).isNotNull();
      assertThat(response.orderItemInfoList()).hasSize(2);
      assertThat(response.paymentInfo()).hasSize(1);

      assertThat(response.orderItemInfoList())
          .extracting("productName")
          .containsExactly("포카칩", "썬칩");
    }

    @Test
    @DisplayName("주문 상세 조회 시 주문이 없으면 ORDER_NOT_FOUND를 반환한다.")
    public void fail_getOrderDetail_orderNotFound() {
      //given
      Order order = mockOrder();
      when(orderRepo.findByUserIdAndOrderId(1L, order.getOrderId())).thenReturn(Optional.empty());

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.getOrderDetail(1L, order.getOrderId()));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 상세 조회 시 주문 아이템 비어 있으면 ORDER_ITEM_NOT_FOUND를 반환한다.")
    public void fail_getOrderDetail_orderItemNotFound() {
      //given
      Order order = mockOrder();
      Payment payment = mockPayment();
      when(orderRepo.findByUserIdAndOrderId(1L, order.getOrderId())).thenReturn(Optional.of(order));
      when(orderItemRepo.findByOrder(order)).thenReturn(Collections.emptyList());
      when(paymentRepo.findByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId())).thenReturn(
          List.of(payment));

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.getOrderDetail(1L, order.getOrderId()));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_ITEM_NOT_FOUND);
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

  @Nested
  @DisplayName("주문 생성/미리보기 공통 검증")
  class OrderValidationTest {

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
  @DisplayName("주문 취소 테스트")
  class OrderCancelTest {

    @Test
    @DisplayName("재고 복구 이벤트를 받으면 주문 상품 기준으로 재고 복구를 요청한다.")
    public void success_cancelOrder() {
      //given
      Order order = mockOrder();

      OrderItem orderItem1 = mockOrderItem(order, 1L, "테스트 상품");
      OrderItem orderItem2 = mockOrderItem(order, 2L, "테스트 상품2");
      when(orderRepo.findByUserIdAndOrderId(1L, order.getOrderId())).thenReturn(Optional.of(order));

      //when

      orderService.cancelOrder(1L, order.getOrderId());

      //then
      verify(order).markCanceled();
      verify(eventPublisher).publishEvent(new OrderStockRestoreRequestEvent(order.getOrderId()));
    }

    @Test
    @DisplayName("PAYMENT_PENDING 상태가 아니면 예외가 발생한다.")
    public void fail_canceledOrder_validate_orderStatus() {
      //given
      Order order = mockOrder();
      
      when(orderRepo.findByUserIdAndOrderId(1L, order.getOrderId())).thenReturn(Optional.of(order));
      when(order.getOrderStatus()).thenReturn(OrderStatus.CANCELED);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.cancelOrder(1L, order.getOrderId()));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_INVALID_STATUS);
    }
  }

  private OrderCreateRequest mockOrderCreateRequest() {
    return new OrderCreateRequest(
        List.of(new OrderItemDto(10L, 2)),
        mockDeliveryRequest(),
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

  private DeliveryRequestDto mockDeliveryRequest() {
    return new DeliveryRequestDto(
        "홍길동",
        "010-1234-5678",
        "06234",
        "서울특별시 강남구 테헤란로",
        "101동 1001호",
        "문 앞에 놔주세요"
    );
  }

  private Order mockOrder() {
    Order order = mock(Order.class);

    lenient().when(order.getOrderId()).thenReturn(1L);
    lenient().when(order.getOrderNumber()).thenReturn("ORD-123");
    lenient().when(order.getOrderTitle()).thenReturn("테스트 상품");
    lenient().when(order.getOrderStatus()).thenReturn(OrderStatus.PAYMENT_PENDING);
    lenient().when(order.getFinalAmount()).thenReturn(10000L);
    lenient().when(order.getCreatedAt()).thenReturn(LocalDateTime.now());

    return order;
  }

  private OrderItem mockOrderItem(Order order, Long itemId, String productName) {
    OrderItem item = mock(OrderItem.class);

    lenient().when(item.getId()).thenReturn(itemId);
    lenient().when(item.getOrder()).thenReturn(order);
    lenient().when(item.getProductName()).thenReturn(productName);
    lenient().when(item.getOptionSummary()).thenReturn(productName + "옵션");
    lenient().when(item.getProductAmount()).thenReturn(10000L);
    lenient().when(item.getQuantity()).thenReturn(1);
    lenient().when(item.getProductImageUrl()).thenReturn(productName + "thumbnail.jpg");

    return item;
  }

  private ProductVariant mockProductVariant() {
    ProductVariant variant = mock(ProductVariant.class);

    return variant;
  }

  private Payment mockPayment() {
    Payment payment = mock(Payment.class);
    lenient().when(payment.getPaymentId()).thenReturn(1L);
    lenient().when(payment.getStatus()).thenReturn(PaymentStatus.PAID);
    lenient().when(payment.getPgProvider()).thenReturn(PgProvider.TOSS_PAYMENTS);
    lenient().when(payment.getTotalAmount()).thenReturn(10000L);
    lenient().when(payment.getRequestedAt()).thenReturn(LocalDateTime.now());
    lenient().when(payment.getApprovedAt()).thenReturn(LocalDateTime.now());

    return payment;
  }

  private OrderPreviewRequest mockOrderPreviewRequest() {
    return new OrderPreviewRequest(
        List.of(new OrderItemDto(10L, 2))
    );
  }
}
