package com.example.WonkaoTalk.domain.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.config.TestContainerConfig;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequest;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequest.DeliveryRequestDto;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderItemDto;
import com.example.WonkaoTalk.domain.order.entity.Delivery;
import com.example.WonkaoTalk.domain.order.entity.DeliveryStatus;
import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import com.example.WonkaoTalk.domain.order.entity.OrderStatus;
import com.example.WonkaoTalk.domain.order.repo.DeliveryRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderRepo;
import com.example.WonkaoTalk.domain.order.service.OrderService;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.store.entity.Store;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.enums.Gender;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.security.oauth2.client.registration.naver.client-id=test-client-id",
    "spring.security.oauth2.client.registration.naver.client-secret=test-client-secret",
    "oauth.encryption.key=1234567890123456"
})
@Import(TestContainerConfig.class)
@Transactional
public class OrderIntegrationTest {

  @Autowired
  OrderService orderService;

  @Autowired
  OrderRepo orderRepo;

  @Autowired
  OrderItemRepo orderItemRepo;

  @Autowired
  DeliveryRepo deliveryRepo;

  @Autowired
  ProductVariantRepo productVariantRepo;

  @Autowired
  EntityManager em;

  @MockitoBean
  OAuth2AuthorizedClientService authorizedClientService;

  @MockitoBean
  ClientRegistrationRepository clientRegistrationRepository;

  @Nested
  @DisplayName("주문 생성 통합 테스트")
  class CreateOrderIntegrationTests {

    @Test
    @DisplayName("주문 생성 시 주문, 주문 상품, 배송정보를 저장하고 재고를 차감한다.")
    void createOrder_SavesOrderItemsDeliveryAndDecreasesStock() {
      // given
      User user = saveUser();
      Store store = saveStore();
      Category category = saveCategory();
      Product product = saveProduct(store, category);
      ProductVariant variant = saveVariant(product, "기본", 10);

      em.flush();
      em.clear();

      Long userId = user.getId();
      OrderCreateRequest request = createOrderCreateRequest(
          List.of(createOrderItemDto(variant.getId(), 2))
      );

      // when
      OrderCreateResponse response = orderService.createOrder(userId, request);

      em.flush();
      em.clear();

      // then
      Order savedOrder = orderRepo.findById(response.orderInfo().orderId()).orElseThrow();
      List<OrderItem> savedOrderItems = orderItemRepo.findByOrder(savedOrder);
      List<Delivery> savedDeliveries = deliveryRepo.findAll();
      ProductVariant savedVariant = productVariantRepo.findById(variant.getId()).orElseThrow();

      assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
      assertThat(savedOrder.getOrderTitle()).isEqualTo("테스트 상품 이름");
      assertThat(savedOrder.getOriginalAmount()).isEqualTo(20000L);
      assertThat(savedOrder.getDiscountAmount()).isEqualTo(4000L);
      assertThat(savedOrder.getPointUsedAmount()).isEqualTo(0L);
      assertThat(savedOrder.getFinalAmount()).isEqualTo(16000L);

      assertThat(savedOrderItems).hasSize(1);
      OrderItem savedOrderItem = savedOrderItems.getFirst();
      assertThat(savedOrderItem.getProductVariant().getId()).isEqualTo(variant.getId());
      assertThat(savedOrderItem.getProductName()).isEqualTo("테스트 상품 이름");
      assertThat(savedOrderItem.getOptionSummary()).isEqualTo("기본");
      assertThat(savedOrderItem.getProductAmount()).isEqualTo(8000L);
      assertThat(savedOrderItem.getQuantity()).isEqualTo(2);
      assertThat(savedOrderItem.getProductImageUrl()).isEqualTo("thumbnail.jpg");

      assertThat(savedDeliveries).hasSize(1);
      Delivery savedDelivery = savedDeliveries.getFirst();
      assertThat(savedDelivery.getOrder().getOrderId()).isEqualTo(savedOrder.getOrderId());
      assertThat(savedDelivery.getRecipientName()).isEqualTo("홍길동");
      assertThat(savedDelivery.getRecipientPhone()).isEqualTo("010-1234-5678");
      assertThat(savedDelivery.getZipcode()).isEqualTo("06234");
      assertThat(savedDelivery.getAddress()).isEqualTo("서울특별시 강남구 테헤란로");
      assertThat(savedDelivery.getAddressDetail()).isEqualTo("101동 1001호");
      assertThat(savedDelivery.getMemo()).isEqualTo("문 앞에 놔주세요");
      assertThat(savedDelivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.PREPARING);

      assertThat(savedVariant.getStock()).isEqualTo(8);
    }

    @Test
    @DisplayName("재고가 부족할 경우 주문이 생성되지 않고 재고가 차감되지 않는다.")
    public void createOrder_StockInsufficient_RollsBackOrderCreation() {
      //given
      User user = saveUser();
      Store store = saveStore();
      Category category = saveCategory();
      Product product = saveProduct(store, category);
      ProductVariant productVariant = saveVariant(product, "기본옵션", 2);

      em.flush();
      em.clear();

      Long userId = user.getId();
      OrderCreateRequest request = createOrderCreateRequest(
          List.of(createOrderItemDto(productVariant.getId(), 3))
      );

      //when
      BusinessException exception = assertThrows(
          BusinessException.class,
          () -> orderService.createOrder(userId, request)
      );

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROD_STOCK_INSUFFICIENT);
      assertThat(orderRepo.findAll()).isEmpty();
      assertThat(orderItemRepo.findAll()).isEmpty();
      assertThat(deliveryRepo.findAll()).isEmpty();

      ProductVariant savedVariant = productVariantRepo.findById(productVariant.getId())
          .orElseThrow();
      assertThat(savedVariant.getStock()).isEqualTo(2);
    }

    @Test
    @DisplayName("판매 불가 옵션이면 주문이 생성되지 않는다")
    public void createOrder_UnavailableVariant_DoesNotCreateOrder() {
      //given
      User user = saveUser();
      Store store = saveStore();
      Category category = saveCategory();
      Product product = saveProduct(store, category);

      ProductVariant productVariant = ProductVariant.builder()
          .product(product)
          .name("기본 옵션")
          .stock(10)
          .status(SaleStatus.STOP_SALE)
          .build();
      em.persist(productVariant);

      em.flush();
      em.clear();

      Long userId = user.getId();
      OrderCreateRequest request = createOrderCreateRequest(
          List.of(createOrderItemDto(productVariant.getId(), 2))
      );

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.createOrder(userId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROD_VARIANT_UNAVAILABLE);
      assertThat(orderRepo.findAll()).isEmpty();
      assertThat(orderItemRepo.findAll()).isEmpty();
      assertThat(deliveryRepo.findAll()).isEmpty();

      ProductVariant savedVariant = productVariantRepo.findById(productVariant.getId())
          .orElseThrow();
      assertThat(savedVariant.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("옵션이 존재하지 않으면 오류가 발생한다.")
    public void createOrder_UnknownVariant_ThrowsNotFound() {
      //given
      User user = saveUser();

      em.flush();
      em.clear();

      Long userId = user.getId();
      OrderCreateRequest request = createOrderCreateRequest(
          List.of(createOrderItemDto(1L, 2))
      );

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> orderService.createOrder(userId, request));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      assertThat(orderRepo.findAll()).isEmpty();
      assertThat(orderItemRepo.findAll()).isEmpty();
      assertThat(deliveryRepo.findAll()).isEmpty();

    }
  }

  private User saveUser() {
    User user = User.builder()
        .nickname("TestNickName")
        .image("profileImg.jpg")
        .birthDate(LocalDate.now())
        .name("TestName")
        .phone("01012341234")
        .gender(Gender.MALE)
        .marketingAgree(false)
        .auth(saveAuth())
        .build();
    em.persist(user);

    return user;
  }

  private Auth saveAuth() {
    Auth auth = Auth.builder().build();
    em.persist(auth);

    return auth;
  }

  private Store saveStore() {
    Seller seller = saveSeller();
    Store store = Store.builder()
        .name("테스트 스토어")
        .description("테스트 스토어 설명")
        .phone("010-0000-0000")
        .seller(seller)
        .build();
    em.persist(store);

    return store;
  }

  private Seller saveSeller() {
    Seller seller = Seller.builder()
        .auth(saveAuth())
        .buzNo(String.valueOf(System.nanoTime()).substring(0, 10))
        .name("테스트 판매자")
        .phone("010-1111-2222")
        .build();
    em.persist(seller);

    return seller;
  }

  private Category saveCategory() {
    Category category = new Category();
    ReflectionTestUtils.setField(category, "name", "테스트 카테고리");
    ReflectionTestUtils.setField(category, "depth", 1);
    em.persist(category);

    return category;
  }

  private Product saveProduct(Store store, Category category) {
    Product product = Product.builder()
        .store(store)
        .category(category)
        .name("테스트 상품 이름")
        .thumbnail("thumbnail.jpg")
        .price(10000)
        .discountedPrice(8000)
        .status(SaleStatus.ON_SALE)
        .likeCount(0)
        .build();
    em.persist(product);

    return product;
  }

  private ProductVariant saveVariant(Product product, String name, int stock) {
    ProductVariant variant = ProductVariant.builder()
        .product(product)
        .name(name)
        .stock(stock)
        .status(SaleStatus.ON_SALE)
        .build();
    em.persist(variant);

    return variant;
  }

  private OrderCreateRequest createOrderCreateRequest(List<OrderItemDto> items) {
    return new OrderCreateRequest(
        items,
        createDeliveryDto(),
        0L
    );
  }

  private DeliveryRequestDto createDeliveryDto() {
    return new DeliveryRequestDto(
        "홍길동",
        "010-1234-5678",
        "06234",
        "서울특별시 강남구 테헤란로",
        "101동 1001호",
        "문 앞에 놔주세요"
    );
  }

  private OrderItemDto createOrderItemDto(Long variantId, int quantity) {
    return new OrderItemDto(variantId, quantity);
  }
}
