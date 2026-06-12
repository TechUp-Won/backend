package com.example.WonkaoTalk.domain.payment.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.config.TestContainerConfig;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import com.example.WonkaoTalk.domain.order.entity.OrderStatus;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderRepo;
import com.example.WonkaoTalk.domain.payment.client.TossPaymentConfirmResult;
import com.example.WonkaoTalk.domain.payment.client.TossPaymentsClient;
import com.example.WonkaoTalk.domain.payment.client.TossPaymentsException;
import com.example.WonkaoTalk.domain.payment.dto.PaymentCheckoutResponse;
import com.example.WonkaoTalk.domain.payment.dto.PaymentConfirmRequest;
import com.example.WonkaoTalk.domain.payment.dto.PaymentConfirmResponse;
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

// flyway 로 db erd 마무리 되면 그때 제거할것
@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.security.oauth2.client.registration.naver.client-id=test-client-id",
    "spring.security.oauth2.client.registration.naver.client-secret=test-client-secret",
    "oauth.encryption.key=1234567890123456"
})
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

  @MockitoBean
  private OAuth2AuthorizedClientService authorizedClientService;

  @MockitoBean
  private ClientRegistrationRepository clientRegistrationRepository;

  @Nested
  @DisplayName("checkout 통합 테스트")
  class CheckoutIntegrationTests {

    @Test
    @DisplayName("결제창 정보를 반환하고 PENDING 상태의 Payment를 저장한다.")
    void success_getCheckout() {
      // given
      Order order = saveOrderFixture("ORD-CHECKOUT-001", OrderStatus.PAYMENT_PENDING);

      // when
      PaymentCheckoutResponse response = paymentService.getCheckout(
          order.getUser().getId(),
          order.getOrderId()
      );

      // then
      Payment savedPayment = paymentRepo.findById(response.paymentId()).orElseThrow();

      assertThat(savedPayment.getOrder().getOrderId()).isEqualTo(order.getOrderId());
      assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
      assertThat(savedPayment.getPaymentKey()).isNull();
      assertThat(savedPayment.getTossOrderId()).startsWith(order.getOrderNumber() + "PAY");
      assertThat(savedPayment.getIdempotencyKey()).isNotBlank();
      assertThat(savedPayment.getTotalAmount()).isEqualTo(35000L);

      assertThat(response.clientKey()).isNotBlank();
      assertThat(response.tossOrderId()).isEqualTo(savedPayment.getTossOrderId());
      assertThat(response.amount()).isEqualTo(35000L);
      assertThat(response.orderName()).isEqualTo("초콜릿");
      assertThat(response.successUrl()).isNotBlank();
      assertThat(response.failUrl()).isNotBlank();
    }
  }

  @Nested
  @DisplayName("confirm 통합 테스트")
  class ConfirmIntegrationTests {

    @Test
    @DisplayName("토스 승인 성공 시 Payment와 Order를 결제 완료 상태로 변경한다.")
    void success_confirm() {
      // given
      Payment payment = savePendingPaymentFixture("ORD-CONFIRM-001", "ORD-CONFIRM-001-PAY-001");
      PaymentConfirmRequest request = new PaymentConfirmRequest(
          "payment-key",
          payment.getTossOrderId(),
          35000L
      );
      TossPaymentConfirmResult result = new TossPaymentConfirmResult(
          "payment-key",
          payment.getTossOrderId(),
          "DONE",
          "CARD",
          35000L,
          "2026-06-12T10:00:00+09:00",
          new TossPaymentConfirmResult.Receipt("https://receipt.example/1")
      );
      when(tossPaymentsClient.confirm(
          request.paymentKey(),
          request.orderId(),
          35000L,
          payment.getIdempotencyKey()
      )).thenReturn(result);

      // when
      PaymentConfirmResponse response = paymentService.confirm(
          payment.getOrder().getUser().getId(),
          request
      );

      // then
      Payment savedPayment = paymentRepo.findById(payment.getPaymentId()).orElseThrow();
      Order savedOrder = orderRepo.findById(payment.getOrder().getOrderId()).orElseThrow();

      assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
      assertThat(savedPayment.getPaymentKey()).isEqualTo("payment-key");
      assertThat(savedPayment.getApprovedAt()).isNotNull();
      assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);

      assertThat(response.paymentId()).isEqualTo(savedPayment.getPaymentId());
      assertThat(response.orderId()).isEqualTo(savedOrder.getOrderId());
      assertThat(response.paymentKey()).isEqualTo("payment-key");
      assertThat(response.status()).isEqualTo(PaymentStatus.PAID.name());
      assertThat(response.receiptUrl()).isEqualTo("https://receipt.example/1");
    }

    @Test
    @DisplayName("토스 승인 실패 시 Payment 실패 기록을 DB에 남기고 Order 상태는 변경하지 않는다.")
    void confirm_RecordFailure_WhenTossConfirmFails() {
      // given
      Payment payment = savePendingPaymentFixture("ORD-CONFIRM-FAIL-001",
          "ORD-CONFIRM-FAIL-001-PAY-001");
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
      assertThatThrownBy(
          () -> paymentService.confirm(payment.getOrder().getUser().getId(), request))
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

    @Test
    @DisplayName("결제 요청 금액이 일치하지 않으면 Payment를 INVALID 상태로 기록한다.")
    void confirm_RecordInvalid_WhenRequestAmountMismatch() {
      // given
      Payment payment = savePendingPaymentFixture("ORD-AMOUNT-MISMATCH-001",
          "ORD-AMOUNT-MISMATCH-001-PAY-001");
      PaymentConfirmRequest request = new PaymentConfirmRequest(
          "payment-key",
          payment.getTossOrderId(),
          34000L
      );

      // when
      assertThatThrownBy(
          () -> paymentService.confirm(payment.getOrder().getUser().getId(), request))
          .isInstanceOf(BusinessException.class)
          .satisfies(throwable -> assertThat(((BusinessException) throwable).getErrorCode())
              .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH));

      // then
      Payment savedPayment = paymentRepo.findById(payment.getPaymentId()).orElseThrow();
      Order savedOrder = orderRepo.findById(payment.getOrder().getOrderId()).orElseThrow();

      assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.INVALID);
      assertThat(savedPayment.getFailCode()).isEqualTo("AMOUNT_MISMATCH");
      assertThat(savedPayment.getFailMessage()).isEqualTo("결제 금액이 일치하지 않습니다.");
      assertThat(savedPayment.getFailedAt()).isNotNull();
      assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("토스 승인 주문번호가 일치하지 않으면 Payment를 INVALID 상태로 기록한다.")
    void confirm_RecordInvalid_WhenTossOrderIdMismatch() {
      // given
      Payment payment = savePendingPaymentFixture("ORD-TOSS-ORDER-MISMATCH-001",
          "ORD-TOSS-ORDER-MISMATCH-001-PAY-001");
      PaymentConfirmRequest request = new PaymentConfirmRequest(
          "payment-key",
          payment.getTossOrderId(),
          35000L
      );
      when(tossPaymentsClient.confirm(
          request.paymentKey(),
          request.orderId(),
          35000L,
          payment.getIdempotencyKey()
      )).thenReturn(createTossConfirmResult(payment, "OTHER-TOSS-ORDER-ID", "DONE", 35000L));

      // when
      assertThatThrownBy(
          () -> paymentService.confirm(payment.getOrder().getUser().getId(), request))
          .isInstanceOf(BusinessException.class)
          .satisfies(throwable -> assertThat(((BusinessException) throwable).getErrorCode())
              .isEqualTo(ErrorCode.PAYMENT_ORDER_MISMATCH));

      // then
      Payment savedPayment = paymentRepo.findById(payment.getPaymentId()).orElseThrow();
      Order savedOrder = orderRepo.findById(payment.getOrder().getOrderId()).orElseThrow();

      assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.INVALID);
      assertThat(savedPayment.getFailCode()).isEqualTo("TOSS_ORDER_ID_MISMATCH");
      assertThat(savedPayment.getFailMessage()).isEqualTo("토스페이먼츠 승인 주문번호가 일치하지 않습니다.");
      assertThat(savedPayment.getFailedAt()).isNotNull();
      assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("토스 승인 상태가 DONE이 아니면 Payment를 FAILED 상태로 기록한다.")
    void confirm_RecordFailure_WhenTossStatusIsNotDone() {
      // given
      Payment payment = savePendingPaymentFixture("ORD-TOSS-STATUS-FAIL-001",
          "ORD-TOSS-STATUS-FAIL-001-PAY-001");
      PaymentConfirmRequest request = new PaymentConfirmRequest(
          "payment-key",
          payment.getTossOrderId(),
          35000L
      );
      when(tossPaymentsClient.confirm(
          request.paymentKey(),
          request.orderId(),
          35000L,
          payment.getIdempotencyKey()
      )).thenReturn(createTossConfirmResult(payment, payment.getTossOrderId(), "CANCELED",
          35000L));

      // when
      assertThatThrownBy(
          () -> paymentService.confirm(payment.getOrder().getUser().getId(), request))
          .isInstanceOf(BusinessException.class)
          .satisfies(throwable -> assertThat(((BusinessException) throwable).getErrorCode())
              .isEqualTo(ErrorCode.PAYMENT_APPROVAL_FAILED));

      // then
      Payment savedPayment = paymentRepo.findById(payment.getPaymentId()).orElseThrow();
      Order savedOrder = orderRepo.findById(payment.getOrder().getOrderId()).orElseThrow();

      assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
      assertThat(savedPayment.getFailCode()).isEqualTo("TOSS_STATUS_NOT_DONE");
      assertThat(savedPayment.getFailMessage()).isEqualTo("토스페이먼츠 결제 상태가 DONE이 아닙니다.");
      assertThat(savedPayment.getFailedAt()).isNotNull();
      assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }
  }

  private Order saveOrderFixture(String orderNumber, OrderStatus orderStatus) {
    return transactionTemplate.execute(status -> {
      User user = saveUser();
      Store store = saveStore();
      Category category = saveCategory();
      Product product = saveProduct(store, category);
      ProductVariant variant = saveVariant(product);

      Order order = Order.createOrder(
          orderNumber,
          user,
          35000L,
          0L,
          0L,
          35000L,
          "초콜릿"
      );
      if (orderStatus == OrderStatus.PAYMENT_PENDING) {
        order.markPaymentPending();
      }
      orderRepo.save(order);

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

      em.flush();
      return order;
    });
  }

  private Payment savePendingPaymentFixture(String orderNumber, String tossOrderId) {
    return transactionTemplate.execute(status -> {
      Order order = saveOrderFixture(orderNumber, OrderStatus.PAYMENT_PENDING);
      Payment payment = Payment.createPendingPayment(
          order,
          tossOrderId,
          "idem-" + tossOrderId,
          35000L,
          LocalDateTime.now()
      );
      paymentRepo.save(payment);
      em.flush();
      return payment;
    });
  }

  private TossPaymentConfirmResult createTossConfirmResult(
      Payment payment,
      String tossOrderId,
      String status,
      Long totalAmount
  ) {
    return new TossPaymentConfirmResult(
        "payment-key",
        tossOrderId,
        status,
        "CARD",
        totalAmount,
        "2026-06-12T10:00:00+09:00",
        new TossPaymentConfirmResult.Receipt(
            "https://receipt.example/" + payment.getPaymentId()
        )
    );
  }

  private User saveUser() {
    Auth auth = Auth.builder().build();
    em.persist(auth);

    String uniqueSuffix = String.valueOf(System.nanoTime()).substring(0, 8);
    User user = User.builder()
        .auth(auth)
        .nickname("테스트유저")
        .name("테스트유저")
        .phone("010" + uniqueSuffix)
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
