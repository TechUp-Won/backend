package com.example.WonkaoTalk.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.seller.repo.SellerRepo;
import com.example.WonkaoTalk.domain.store.entity.Store;
import com.example.WonkaoTalk.domain.store.repo.StoreRepo;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductDeleteServiceTest {

  private static final Long AUTH_ID = 1L;
  private static final Long PRODUCT_ID = 101L;
  private static final Long STORE_ID = 7L;

  @Mock private SellerRepo sellerRepo;
  @Mock private StoreRepo storeRepo;
  @Mock private ProductRepo productRepo;
  @Mock private OrderItemRepo orderItemRepo;

  @InjectMocks private ProductDeleteService productDeleteService;

  private Store store;
  private Product product;

  @BeforeEach
  void setUp() {
    Seller seller = mock(Seller.class);
    store = mock(Store.class);
    product = mock(Product.class);

    when(store.getId()).thenReturn(STORE_ID);
    when(sellerRepo.findByAuthId(AUTH_ID)).thenReturn(Optional.of(seller));
    when(storeRepo.findBySeller(seller)).thenReturn(Optional.of(store));
    when(productRepo.findByIdWithLock(PRODUCT_ID)).thenReturn(Optional.of(product));
    when(product.getStore()).thenReturn(store);
    when(product.getDeletedAt()).thenReturn(null);
    when(orderItemRepo.existsActiveOrderByProductId(eq(PRODUCT_ID), any())).thenReturn(false);
  }

  // ── 인증/권한 실패 ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("판매자를 찾을 수 없으면 SELLER_NOT_FOUND를 던진다")
  void delete_throwsException_whenSellerNotFound() {
    when(sellerRepo.findByAuthId(anyLong())).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productDeleteService.delete(AUTH_ID, PRODUCT_ID));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SELLER_NOT_FOUND);
  }

  @Test
  @DisplayName("스토어를 찾을 수 없으면 PROD_STORE_NOT_FOUND를 던진다")
  void delete_throwsException_whenStoreNotFound() {
    when(storeRepo.findBySeller(any())).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productDeleteService.delete(AUTH_ID, PRODUCT_ID));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_STORE_NOT_FOUND);
  }

  // ── 상품 조회 실패 ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("상품을 찾을 수 없으면 PROD_NOT_FOUND를 던진다")
  void delete_throwsException_whenProductNotFound() {
    when(productRepo.findByIdWithLock(PRODUCT_ID)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productDeleteService.delete(AUTH_ID, PRODUCT_ID));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_NOT_FOUND);
  }

  @Test
  @DisplayName("이미 삭제된 상품이면 PROD_DELETED를 던진다")
  void delete_throwsException_whenProductAlreadyDeleted() {
    when(product.getDeletedAt()).thenReturn(LocalDateTime.now());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productDeleteService.delete(AUTH_ID, PRODUCT_ID));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_DELETED);
  }

  @Test
  @DisplayName("다른 판매자의 상품이면 FORBIDDEN을 던진다")
  void delete_throwsException_whenProductBelongsToOtherStore() {
    Store otherStore = mock(Store.class);
    when(otherStore.getId()).thenReturn(999L);
    when(product.getStore()).thenReturn(otherStore);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productDeleteService.delete(AUTH_ID, PRODUCT_ID));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  // ── 주문 검증 실패 ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("진행 중인 주문이 있으면 PROD_HAS_ACTIVE_ORDER를 던진다")
  void delete_throwsException_whenActiveOrderExists() {
    when(orderItemRepo.existsActiveOrderByProductId(eq(PRODUCT_ID), any())).thenReturn(true);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productDeleteService.delete(AUTH_ID, PRODUCT_ID));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_HAS_ACTIVE_ORDER);
  }

  // ── 성공 ──────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("정상 삭제 시 상품의 softDelete가 호출된다")
  void delete_callsSoftDelete_onSuccess() {
    productDeleteService.delete(AUTH_ID, PRODUCT_ID);

    verify(product).softDelete();
  }

  @Test
  @DisplayName("진행 중 주문 상태 3개(CREATED, PAYMENT_PENDING, PAID)를 검증 대상으로 사용한다")
  void delete_checksThreeActiveOrderStatuses() {
    productDeleteService.delete(AUTH_ID, PRODUCT_ID);

    verify(orderItemRepo).existsActiveOrderByProductId(eq(PRODUCT_ID), any());
  }

  @Test
  @DisplayName("진행 중인 주문이 없으면 softDelete를 호출한다")
  void delete_proceedsToSoftDelete_whenNoActiveOrders() {
    when(orderItemRepo.existsActiveOrderByProductId(eq(PRODUCT_ID), any())).thenReturn(false);

    productDeleteService.delete(AUTH_ID, PRODUCT_ID);

    verify(product).softDelete();
  }

  @Test
  @DisplayName("진행 중인 주문이 있으면 softDelete를 호출하지 않는다")
  void delete_doesNotCallSoftDelete_whenActiveOrderExists() {
    when(orderItemRepo.existsActiveOrderByProductId(eq(PRODUCT_ID), any())).thenReturn(true);

    assertThrows(BusinessException.class, () -> productDeleteService.delete(AUTH_ID, PRODUCT_ID));

    verify(product, never()).softDelete();
  }
}
