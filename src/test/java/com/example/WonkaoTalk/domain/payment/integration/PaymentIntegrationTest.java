package com.example.WonkaoTalk.domain.payment.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.config.TestContainerConfig;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import com.example.WonkaoTalk.domain.order.entity.OrderStatus;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderRepo;
import com.example.WonkaoTalk.domain.payment.client.TossPaymentsClient;
import com.example.WonkaoTalk.domain.payment.client.TossPaymentsException;
import com.example.WonkaoTalk.domain.payment.dto.PaymentConfirmRequest;
import com.example.WonkaoTalk.domain.payment.entity.Payment;
import com.example.WonkaoTalk.domain.payment.entity.PaymentStatus;
import com.example.WonkaoTalk.domain.payment.repo.PaymentRepo;
import com.example.WonkaoTalk.domain.payment.service.PaymentService;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.store.entity.Store;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.enums.Gender;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

// flyway 로 db erd 마무리 되면 그때 제거할것
@SpringBootTest(properties = "spring.flyway.enabled=false")
@Import(TestContainerConfig.class)
class PaymentIntegrationTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private OrderRepo orderRepo;

  @Autowired
  private OrderItemRepo orderItemRepo;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private PaymentRepo paymentRepo;

  @MockitoBean
  private TossPaymentsClient tossPaymentsClient;

  @Test
  @DisplayName("토스 승인 실패 시 Payment와 Order 실패 기록을 DB에 남긴다")
  void confirm_RecordFailure_WhenTossConfirmFails() {
    // given
    // payment 객체 생성
    Payment payment = transactionTemplate.execute(status -> {
      User user = saveUser();
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
      orderRepo.save(order);
//      em.persist(order);

      OrderItem orderItem = OrderItem.createOrderItem(
          order,
          variant,
          "초콜릿",
          "기본",
          35000L,
          1,
          "https://image.example/choco.png"
      );
      orderItemRepo.save(orderItem);
//      em.persist(orderItem);

      Payment readyPayment = Payment.createPendingPayment(
          order,
          "ORD-TEST-001-PAY-001",
          "idem-key",
          35000L,
          LocalDateTime.now()
      );
      paymentRepo.save(readyPayment);
//      em.persist(readyPayment);
      em.flush();
      return readyPayment;
    });

    PaymentConfirmRequest request = new PaymentConfirmRequest(
        "payment-key",
        payment.getTossOrderId(),
        35000L
    );
    when(tossPaymentsClient.confirm(
        "payment-key",
        payment.getTossOrderId(),
        35000L,
        payment.getIdempotencyKey()
    )).thenThrow(new TossPaymentsException(
        "REJECT_CARD",
        "카드 승인이 거절되었습니다."
    ));

    // when
    assertThatThrownBy(() -> paymentService.confirm(payment.getOrder().getUser().getId(), request))
        .isInstanceOf(BusinessException.class);

    // then
    Payment savedPayment = paymentRepo.findById(payment.getPaymentId()).orElseThrow();
    Order savedOrder = orderRepo.findById(payment.getOrder().getOrderId()).orElseThrow();
    assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(savedPayment.getFailCode()).isEqualTo("REJECT_CARD");
    assertThat(savedPayment.getFailMessage()).isEqualTo("카드 승인이 거절되었습니다.");
    assertThat(savedPayment.getFailedAt()).isNotNull();
    assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
  }

  private User saveUser() {
    Auth auth = Auth.builder().build();
    em.persist(auth);

    User user = User.builder()
        .auth(auth)
        .nickname("테스트유저")
        .name("테스트유저")
        .phone("01012345678")
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
}
