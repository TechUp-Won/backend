package com.example.WonkaoTalk.domain.order.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequest;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequest.DeliveryRequestDto;
import com.example.WonkaoTalk.domain.order.dto.OrderItemDto;
import com.example.WonkaoTalk.domain.order.entity.Delivery;
import com.example.WonkaoTalk.domain.order.entity.DeliveryStatus;
import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.repo.DeliveryRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderRepo;
import com.example.WonkaoTalk.domain.payment.entity.Payment;
import com.example.WonkaoTalk.domain.payment.service.PaymentService;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
  private PaymentService paymentService;

  @InjectMocks
  private OrderService orderService;

  @Test
  @DisplayName("중복 variantId가 있으면 Bad_Request 예외를 던진다.")
  public void DuplicatedVariantId() {
    //given
    OrderItemDto item1 = new OrderItemDto(1L, 1);
    OrderItemDto item2 = new OrderItemDto(1L, 2);

    //then
    assertThatThrownBy(() -> orderService.validateDuplicateVariant(List.of(item1, item2)))
        .isInstanceOf(BusinessException.class)
        .hasMessage(ErrorCode.BAD_REQUEST.getMessage());
  }

  @Test
  @DisplayName("중복 variantId가 없으면 예외가 발생하지 않는다.")
  public void Duplication_DoesNotThrowException_WhenVariantIdNotDuplicated() {
    //given
    OrderItemDto item1 = new OrderItemDto(1L, 1);
    OrderItemDto item2 = new OrderItemDto(2L, 2);

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
    OrderItemDto item1 = new OrderItemDto(1L, 1);
    OrderItemDto item2 = new OrderItemDto(2L, 2);

    ProductVariant variant1 = mock(ProductVariant.class);
    ProductVariant variant2 = mock(ProductVariant.class);
    Map<Long, ProductVariant> variantMap = Map.of(1L, variant1, 2L, variant2);

    //when
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
    OrderItemDto item1 = new OrderItemDto(1L, 1);
    OrderItemDto item2 = new OrderItemDto(2L, 2);

    ProductVariant variant1 = mock(ProductVariant.class);
    ProductVariant variant2 = mock(ProductVariant.class);
    Map<Long, ProductVariant> variantMap = Map.of(1L, variant1, 2L, variant2);

    //when
    when(variant1.getStock()).thenReturn(10);
    when(variant2.getStock()).thenReturn(1);

    //then
    assertThatThrownBy(() -> orderService.validateVariantStock(List.of(item1, item2), variantMap))
        .isInstanceOf(BusinessException.class)
        .hasMessage(ErrorCode.PROD_STOCK_INSUFFICIENT.getMessage());
  }

  @Test
  @DisplayName("주문 생성 시 요청 배송 정보를 Delivery로 저장한다.")
  public void createOrder_SaveDeliveryInfo() {
    // given
    User user = mock(User.class);
    Product product = mock(Product.class);
    ProductVariant variant = mock(ProductVariant.class);

    OrderCreateRequest request = new OrderCreateRequest(
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

    when(userRepo.findById(1L)).thenReturn(Optional.of(user));
    when(productVariantRepo.findAllById(List.of(10L))).thenReturn(List.of(variant));
    when(orderRepo.existsByOrderNumber(any())).thenReturn(false);
    when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(orderItemRepo.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    when(paymentService.createReadyPayment(any(Order.class))).thenAnswer(invocation ->
        Payment.createReadyPayment(invocation.getArgument(0), "ORD-TEST-PAY", "idem-key", 20000L,
            LocalDateTime.now()));

    when(variant.getId()).thenReturn(10L);
    when(variant.getStock()).thenReturn(10);
    when(variant.getStatus()).thenReturn(SaleStatus.ON_SALE);
    when(variant.getProduct()).thenReturn(product);
    when(variant.getName()).thenReturn("검정");

    when(product.getId()).thenReturn(100L);
    when(product.getName()).thenReturn("테스트 상품");
    when(product.getThumbnail()).thenReturn("thumbnail.jpg");
    when(product.getPrice()).thenReturn(12000);
    when(product.getDiscountedPrice()).thenReturn(10000);

    // when
    orderService.createOrder(1L, request);

    // then
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
  }

}
