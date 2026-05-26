package com.example.WonkaoTalk.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.product.dto.StockAdjustRequest;
import com.example.WonkaoTalk.domain.product.dto.StockAdjustResponse;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.entity.StockHistory;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.enums.StockChangeReason;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import com.example.WonkaoTalk.domain.product.repo.StockHistoryRepo;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockAdjustServiceTest {

  private static final Long AUTH_ID = 1L;
  private static final Long PRODUCT_ID = 101L;
  private static final Long VARIANT_ID = 12L;
  private static final Long STORE_ID = 7L;

  @Mock private SellerRepo sellerRepo;
  @Mock private StoreRepo storeRepo;
  @Mock private ProductRepo productRepo;
  @Mock private ProductVariantRepo productVariantRepo;
  @Mock private StockHistoryRepo stockHistoryRepo;

  @InjectMocks private StockAdjustService stockAdjustService;

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
    when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    when(product.getStore()).thenReturn(store);
    when(product.getDeletedAt()).thenReturn(null);
    when(stockHistoryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  // ── 입력값 검증 실패 ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("changeAmount가 0이면 BAD_REQUEST를 던진다")
  void adjust_throwsException_whenChangeAmountIsZero() {
    StockAdjustRequest request = new StockAdjustRequest(0, StockChangeReason.RESTOCK);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  @DisplayName("허용되지 않는 reason(SALE)이면 BAD_REQUEST를 던진다")
  void adjust_throwsException_whenReasonIsSale() {
    StockAdjustRequest request = new StockAdjustRequest(10, StockChangeReason.SALE);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  @DisplayName("허용되지 않는 reason(CANCEL)이면 BAD_REQUEST를 던진다")
  void adjust_throwsException_whenReasonIsCancel() {
    StockAdjustRequest request = new StockAdjustRequest(10, StockChangeReason.CANCEL);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  @DisplayName("허용되지 않는 reason(INITIAL_STOCK)이면 BAD_REQUEST를 던진다")
  void adjust_throwsException_whenReasonIsInitialStock() {
    StockAdjustRequest request = new StockAdjustRequest(10, StockChangeReason.INITIAL_STOCK);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  // ── 인증/권한 실패 ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("판매자를 찾을 수 없으면 SELLER_NOT_FOUND를 던진다")
  void adjust_throwsException_whenSellerNotFound() {
    when(sellerRepo.findByAuthId(anyLong())).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, restockRequest(10)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SELLER_NOT_FOUND);
  }

  @Test
  @DisplayName("스토어를 찾을 수 없으면 PROD_STORE_NOT_FOUND를 던진다")
  void adjust_throwsException_whenStoreNotFound() {
    when(storeRepo.findBySeller(any())).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, restockRequest(10)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_STORE_NOT_FOUND);
  }

  // ── 상품 조회 실패 ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("상품을 찾을 수 없으면 PROD_NOT_FOUND를 던진다")
  void adjust_throwsException_whenProductNotFound() {
    when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, restockRequest(10)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_NOT_FOUND);
  }

  @Test
  @DisplayName("삭제된 상품이면 PROD_DELETED를 던진다")
  void adjust_throwsException_whenProductIsDeleted() {
    when(product.getDeletedAt()).thenReturn(LocalDateTime.now());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, restockRequest(10)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_DELETED);
  }

  @Test
  @DisplayName("다른 판매자의 상품이면 FORBIDDEN을 던진다")
  void adjust_throwsException_whenProductBelongsToOtherStore() {
    Store otherStore = mock(Store.class);
    when(otherStore.getId()).thenReturn(999L);
    when(product.getStore()).thenReturn(otherStore);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, restockRequest(10)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  // ── variant 조회 실패 ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("variant를 찾을 수 없으면 PROD_VARIANT_NOT_FOUND를 던진다")
  void adjust_throwsException_whenVariantNotFound() {
    when(productVariantRepo.findByIdAndProductIdWithLock(VARIANT_ID, PRODUCT_ID))
        .thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, restockRequest(10)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_VARIANT_NOT_FOUND);
  }

  // ── 재고 검증 실패 ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("차감 결과 재고가 0 미만이 되면 PROD_STOCK_INSUFFICIENT를 던진다")
  void adjust_throwsException_whenStockBecomesNegative() {
    ProductVariant variant = buildVariant(5, SaleStatus.ON_SALE, SaleStatus.ON_SALE);
    when(productVariantRepo.findByIdAndProductIdWithLock(VARIANT_ID, PRODUCT_ID))
        .thenReturn(Optional.of(variant));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID,
            new StockAdjustRequest(-6, StockChangeReason.ADJUSTMENT)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_STOCK_INSUFFICIENT);
  }

  // ── 성공 — 응답 값 ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("입고 성공 시 응답에 stockBefore, changeAmount, stockAfter가 올바르게 담긴다")
  void adjust_returnsCorrectStockValues_onRestock() {
    ProductVariant variant = buildVariant(30, SaleStatus.ON_SALE, SaleStatus.ON_SALE);
    when(productVariantRepo.findByIdAndProductIdWithLock(VARIANT_ID, PRODUCT_ID))
        .thenReturn(Optional.of(variant));

    StockAdjustResponse response = stockAdjustService.adjust(
        AUTH_ID, PRODUCT_ID, VARIANT_ID, restockRequest(50));

    assertThat(response.stockBefore()).isEqualTo(30);
    assertThat(response.changeAmount()).isEqualTo(50);
    assertThat(response.stockAfter()).isEqualTo(80);
    assertThat(response.reason()).isEqualTo(StockChangeReason.RESTOCK);
  }

  @Test
  @DisplayName("차감 성공 시 응답에 stockBefore, changeAmount, stockAfter가 올바르게 담긴다")
  void adjust_returnsCorrectStockValues_onAdjustment() {
    ProductVariant variant = buildVariant(30, SaleStatus.ON_SALE, SaleStatus.ON_SALE);
    when(productVariantRepo.findByIdAndProductIdWithLock(VARIANT_ID, PRODUCT_ID))
        .thenReturn(Optional.of(variant));

    StockAdjustResponse response = stockAdjustService.adjust(
        AUTH_ID, PRODUCT_ID, VARIANT_ID,
        new StockAdjustRequest(-5, StockChangeReason.ADJUSTMENT));

    assertThat(response.stockBefore()).isEqualTo(30);
    assertThat(response.changeAmount()).isEqualTo(-5);
    assertThat(response.stockAfter()).isEqualTo(25);
  }

  @Test
  @DisplayName("차감 결과 재고가 정확히 0이 되면 성공한다")
  void adjust_succeedsWhenStockBecomesExactlyZero() {
    ProductVariant variant = buildVariant(5, SaleStatus.ON_SALE, SaleStatus.ON_SALE);
    when(productVariantRepo.findByIdAndProductIdWithLock(VARIANT_ID, PRODUCT_ID))
        .thenReturn(Optional.of(variant));

    StockAdjustResponse response = stockAdjustService.adjust(
        AUTH_ID, PRODUCT_ID, VARIANT_ID,
        new StockAdjustRequest(-5, StockChangeReason.ADJUSTMENT));

    assertThat(response.stockAfter()).isEqualTo(0);
  }

  // ── 성공 — variant 상태 변경 ─────────────────────────────────────────────────

  @Test
  @DisplayName("차감 결과 재고가 0이 되면 variant status가 OUT_OF_STOCK으로 변경된다")
  void adjust_changesVariantStatusToOutOfStock_whenStockBecomesZero() {
    ProductVariant variant = buildVariant(5, SaleStatus.ON_SALE, SaleStatus.ON_SALE);
    when(productVariantRepo.findByIdAndProductIdWithLock(VARIANT_ID, PRODUCT_ID))
        .thenReturn(Optional.of(variant));

    stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID,
        new StockAdjustRequest(-5, StockChangeReason.ADJUSTMENT));

    assertThat(variant.getStatus()).isEqualTo(SaleStatus.OUT_OF_STOCK);
  }

  @Test
  @DisplayName("입고 결과 OUT_OF_STOCK이던 variant의 재고가 1 이상이 되면 ON_SALE로 복구된다")
  void adjust_restoresVariantStatusToOnSale_whenStockIncreasesFromZero() {
    ProductVariant variant = buildVariant(0, SaleStatus.OUT_OF_STOCK, SaleStatus.ON_SALE);
    when(productVariantRepo.findByIdAndProductIdWithLock(VARIANT_ID, PRODUCT_ID))
        .thenReturn(Optional.of(variant));

    stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, restockRequest(10));

    assertThat(variant.getStatus()).isEqualTo(SaleStatus.ON_SALE);
  }

  @Test
  @DisplayName("상품이 STOP_SALE이면 입고 후에도 variant status가 OUT_OF_STOCK으로 유지된다")
  void adjust_doesNotRestoreVariantStatus_whenProductIsStopSale() {
    ProductVariant variant = buildVariant(0, SaleStatus.OUT_OF_STOCK, SaleStatus.STOP_SALE);
    when(productVariantRepo.findByIdAndProductIdWithLock(VARIANT_ID, PRODUCT_ID))
        .thenReturn(Optional.of(variant));

    stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, restockRequest(10));

    assertThat(variant.getStatus()).isEqualTo(SaleStatus.OUT_OF_STOCK);
  }

  // ── 성공 — 재고 이력 ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("재고 조정 성공 시 stock_histories에 이력이 저장된다")
  void adjust_savesStockHistory_onSuccess() {
    ProductVariant variant = buildVariant(30, SaleStatus.ON_SALE, SaleStatus.ON_SALE);
    when(productVariantRepo.findByIdAndProductIdWithLock(VARIANT_ID, PRODUCT_ID))
        .thenReturn(Optional.of(variant));

    stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, restockRequest(50));

    verify(stockHistoryRepo).save(any(StockHistory.class));
  }

  @Test
  @DisplayName("저장되는 이력에 변경 전 재고, 변경량, 사유가 올바르게 기록된다")
  void adjust_savesStockHistoryWithCorrectValues() {
    ProductVariant variant = buildVariant(30, SaleStatus.ON_SALE, SaleStatus.ON_SALE);
    when(productVariantRepo.findByIdAndProductIdWithLock(VARIANT_ID, PRODUCT_ID))
        .thenReturn(Optional.of(variant));

    stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, restockRequest(50));

    ArgumentCaptor<StockHistory> captor = ArgumentCaptor.forClass(StockHistory.class);
    verify(stockHistoryRepo).save(captor.capture());
    StockHistory saved = captor.getValue();
    assertThat(saved.getStockBefore()).isEqualTo(30);
    assertThat(saved.getChangeAmount()).isEqualTo(50);
    assertThat(saved.getStockAfter()).isEqualTo(80);
    assertThat(saved.getReason()).isEqualTo(StockChangeReason.RESTOCK);
  }

  @Test
  @DisplayName("판매자가 직접 조정한 이력의 order_id는 null이다")
  void adjust_savesStockHistoryWithNullOrder() {
    ProductVariant variant = buildVariant(30, SaleStatus.ON_SALE, SaleStatus.ON_SALE);
    when(productVariantRepo.findByIdAndProductIdWithLock(VARIANT_ID, PRODUCT_ID))
        .thenReturn(Optional.of(variant));

    stockAdjustService.adjust(AUTH_ID, PRODUCT_ID, VARIANT_ID, restockRequest(50));

    ArgumentCaptor<StockHistory> captor = ArgumentCaptor.forClass(StockHistory.class);
    verify(stockHistoryRepo).save(captor.capture());
    assertThat(captor.getValue().getOrder()).isNull();
  }

  @Test
  @DisplayName("재고 부족이면 이력을 저장하지 않는다")
  void adjust_doesNotSaveHistory_whenStockIsInsufficient() {
    ProductVariant variant = buildVariant(3, SaleStatus.ON_SALE, SaleStatus.ON_SALE);
    when(productVariantRepo.findByIdAndProductIdWithLock(VARIANT_ID, PRODUCT_ID))
        .thenReturn(Optional.of(variant));

    assertThrows(BusinessException.class, () -> stockAdjustService.adjust(
        AUTH_ID, PRODUCT_ID, VARIANT_ID,
        new StockAdjustRequest(-10, StockChangeReason.ADJUSTMENT)));

    verify(stockHistoryRepo, never()).save(any());
  }

  // ── 헬퍼 ──────────────────────────────────────────────────────────────────────

  private StockAdjustRequest restockRequest(int amount) {
    return new StockAdjustRequest(amount, StockChangeReason.RESTOCK);
  }

  private ProductVariant buildVariant(int stock, SaleStatus variantStatus,
      SaleStatus productStatus) {
    Product variantProduct = mock(Product.class);
    when(variantProduct.getStatus()).thenReturn(productStatus);
    return ProductVariant.builder()
        .product(variantProduct)
        .stock(stock)
        .name("화이트 / M")
        .status(variantStatus)
        .build();
  }
}
