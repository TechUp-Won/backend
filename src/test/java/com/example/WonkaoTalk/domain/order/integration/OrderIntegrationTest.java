package com.example.WonkaoTalk.domain.order.integration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.example.WonkaoTalk.config.TestContainerConfig;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequest;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequest.DeliveryRequestDto;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderItemDto;
import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import com.example.WonkaoTalk.domain.order.entity.OrderStatus;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderRepo;
import com.example.WonkaoTalk.domain.order.service.OrderService;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.store.entity.Store;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.enums.Gender;
import jakarta.persistence.EntityManager;
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
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.security.oauth2.client.registration.naver.client-id=test-client-id",
    "spring.security.oauth2.client.registration.naver.client-secret=test-client-secret",
    "oauth.encryption.key=1234567890123456"
})
@Import(TestContainerConfig.class)
public class OrderIntegrationTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private OrderRepo orderRepo;

  @Autowired
  private OrderItemRepo orderItemRepo;

  @Autowired
  private ProductRepo productRepo;

  @Autowired
  private ProductVariantRepo productVariantRepo;

  @Autowired
  private OrderService orderService;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @MockitoBean
  private OAuth2AuthorizedClientService authorizedClientService;

  @MockitoBean
  private ClientRegistrationRepository clientRegistrationRepository;


  @Nested
  @DisplayName("")
  class CreateOrderIntegrationTests {

    @Test
    @DisplayName("주문 생성이 성공이 되고 재고가 차감이 된다.")
    public void success_createOrder() {
      //given
      OrderCreateFixture fixture = transactionTemplate.execute(status -> {
        User user = saveUser("테스트 닉네임", "테스트 이름", "01012341234");
        Store store = saveStore();
        Category category = saveCategory();
        Product product = saveProduct(store, category);
        ProductVariant variant = saveVariant(product);

        em.flush();

        return new OrderCreateFixture(user.getId(), variant.getId());
      });

      List<OrderItemDto> items = List.of(new OrderItemDto(fixture.variantId, 2));
      OrderCreateRequest request = mockOrderCreateRequest(items);

      //when
      OrderCreateResponse response = orderService.createOrder(fixture.userId, request);

      //then
      Order order = orderRepo.findByUserIdAndOrderId(fixture.userId, response.orderInfo().orderId())
          .orElseThrow();
      assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);

      List<OrderItem> orderItemList = orderItemRepo.findByOrder(order);
      assertThat(orderItemList.size()).isEqualTo(1);
      assertThat(orderItemList.get(0).getProductVariant().getId()).isEqualTo(fixture.variantId);

      ProductVariant variant = productVariantRepo.findById(fixture.variantId).orElseThrow();
      assertThat(variant.getStatus()).isEqualTo(SaleStatus.ON_SALE);
      assertThat(variant.getStock()).isEqualTo(8);

    }
  }

  @Nested
  @DisplayName("")
  class CancelOrderIntegrationTests {

    @Test
    @DisplayName("주문 취소 시 주문이 취소 상태로 변경되고 주문 상품 수량만큼 재고가 증가한다.")
    public void success_cancelOrder() {
      //given
      /**
       * 취소 전 만들어져있어야 하는 엔티티들
       *   User
       *   Store
       *   Category
       *   Product
       *   ProductVariant
       *   Order
       *   OrderItem
       */
      OrderCancelFixture fixture = transactionTemplate.execute(status -> {
        User user = saveUser("테스트 닉네임2", "테스트 이름2", "01012345678");
        Store store = saveStore();
        Category category = saveCategory();
        Product product = saveProduct(store, category);
        ProductVariant variant = saveVariant(product);

        Order order = Order.createOrder(
            "ORD-TEST-001",
            user,
            35000L,
            0L,
            0L,
            35000L,
            "초콜릿"
        );
        order.markPaymentPending();
        em.persist(order);

        OrderItem orderItem = OrderItem.createOrderItem(
            order,
            variant,
            "초콜릿",
            "기본",
            35000L,
            1,
            "https://image.example/choco.png"
        );
        em.persist(orderItem);

        em.flush();

        return new OrderCancelFixture(user.getId(), order.getOrderId(), variant.getId());
      });

      //when
      orderService.cancelOrder(fixture.userId(), fixture.orderId());

      //then
      Order savedOrder = orderRepo.findById(fixture.orderId()).orElseThrow();
      assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);

      ProductVariant variant = productVariantRepo.findById(fixture.variantId).orElseThrow();
      assertThat(variant.getStock()).isEqualTo(11);
    }
  }


  private User saveUser(String nickname, String name, String phone) {
    Auth auth = Auth.builder().build();
    em.persist(auth);

    User user = User.builder()
        .auth(auth)
        .nickname(nickname)
        .name(name)
        .phone(phone)
        .gender(Gender.NONE)
        .build();
    em.persist(user);
    return user;
  }

  private Store saveStore() {
    Auth auth = Auth.builder().build();
    em.persist(auth);

    Seller seller = Seller.builder()
        .auth(auth)
        .buzNo(String.valueOf(System.nanoTime()).substring(0, 10))
        .name("테스트판매자")
        .phone("010-0000-0000")
        .build();
    em.persist(seller);

    Store store = Store.builder()
        .seller(seller)
        .name("테스트스토어")
        .description("설명")
        .phone("010-0000-0000")
        .build();
    em.persist(store);
    return store;
  }

  private Category saveCategory() {
    Category cat = new Category();
    ReflectionTestUtils.setField(cat, "name", "테스트카테고리");
    ReflectionTestUtils.setField(cat, "depth", 1);
    em.persist(cat);
    return cat;
  }

  private Product saveProduct(Store store, Category category) {
    Product product = Product.builder()
        .store(store)
        .category(category)
        .name("초콜릿")
        .price(35000)
        .discountedPrice(35000)
        .status(SaleStatus.ON_SALE)
        .likeCount(0)
        .build();
    em.persist(product);
    return product;
  }

  private ProductVariant saveVariant(Product product) {
    ProductVariant variant = ProductVariant.builder()
        .product(product)
        .name("기본")
        .stock(10)
        .status(SaleStatus.ON_SALE)
        .build();
    em.persist(variant);
    return variant;
  }

  private record OrderCancelFixture(
      Long userId,
      Long orderId,
      Long variantId
  ) {

  }

  private record OrderCreateFixture(
      Long userId,
      Long variantId
  ) {

  }

  private OrderCreateRequest mockOrderCreateRequest(List<OrderItemDto> items) {
    return new OrderCreateRequest(
        items,
        mockDeliveryRequest(),
        0L
    );
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

}
