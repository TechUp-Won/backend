package com.example.WonkaoTalk.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.image.service.ImageService;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateRequest.ImageRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateRequest.OptionGroupRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateRequest.OptionRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateRequest.VariantRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateResponse;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.repo.CategoryRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductDetailRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductImageRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionGroupRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import com.example.WonkaoTalk.domain.product.repo.StockHistoryRepo;
import com.example.WonkaoTalk.domain.product.repo.VariantOptionMapRepo;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.seller.repo.SellerRepo;
import com.example.WonkaoTalk.domain.store.entity.Store;
import com.example.WonkaoTalk.domain.store.repo.StoreRepo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductCreateServiceTest {

  private static final Long AUTH_ID = 1L;
  @Mock
  private SellerRepo sellerRepo;
  @Mock
  private StoreRepo storeRepo;
  @Mock
  private CategoryRepo categoryRepo;
  @Mock
  private ProductRepo productRepo;
  @Mock
  private ProductDetailRepo productDetailRepo;
  @Mock
  private ProductImageRepo productImageRepo;
  @Mock
  private ProductOptionGroupRepo productOptionGroupRepo;
  @Mock
  private ProductOptionRepo productOptionRepo;
  @Mock
  private ProductVariantRepo productVariantRepo;
  @Mock
  private VariantOptionMapRepo variantOptionMapRepo;
  @Mock
  private StockHistoryRepo stockHistoryRepo;
  @Mock
  private ImageService imageService;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @InjectMocks
  private ProductCreateService productCreateService;

  @BeforeEach
  void setUp() {
    Seller seller = mock(Seller.class);
    Store store = mock(Store.class);
    Category category = mock(Category.class);

    when(store.getId()).thenReturn(7L);
    when(category.getId()).thenReturn(5L);

    when(sellerRepo.findByAuthId(AUTH_ID)).thenReturn(Optional.of(seller));
    when(storeRepo.findBySeller(seller)).thenReturn(Optional.of(store));
    when(categoryRepo.findById(5L)).thenReturn(Optional.of(category));

    when(productRepo.save(any(Product.class))).thenAnswer(inv -> {
      Product p = inv.getArgument(0);
      ReflectionTestUtils.setField(p, "id", 101L);
      ReflectionTestUtils.setField(p, "createdAt", LocalDateTime.of(2026, 5, 11, 10, 0));
      return p;
    });
    when(productDetailRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(productImageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(productOptionGroupRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(productOptionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(productVariantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(variantOptionMapRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(stockHistoryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    when(imageService.validateAndGetProductUrl(anyString()))
        .thenReturn("http://localhost:9000/wonkaotalk/products/test.jpg");
  }

  // ── 성공 케이스 ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("옵션 없는 상품 등록에 성공한다")
  void createProduct_noOption_success() {
    ProductCreateRequest request = noOptionRequest();

    ProductCreateResponse response = productCreateService.create(AUTH_ID, request);

    assertThat(response.productId()).isEqualTo(101L);
    assertThat(response.storeId()).isEqualTo(7L);
    assertThat(response.name()).isEqualTo("테스트 상품");
    assertThat(response.price()).isEqualTo(10000);
    assertThat(response.discountRate()).isEqualTo(0);
    assertThat(response.discountedPrice()).isEqualTo(10000);
    assertThat(response.status()).isEqualTo("ON_SALE");
    verify(productVariantRepo).save(any());
  }

  @Test
  @DisplayName("옵션 있는 상품 등록에 성공한다")
  void createProduct_withOption_success() {
    ProductCreateRequest request = withOptionRequest();

    ProductCreateResponse response = productCreateService.create(AUTH_ID, request);

    assertThat(response.productId()).isEqualTo(101L);
    verify(productOptionGroupRepo, atLeastOnce()).save(any());
    verify(productOptionRepo, atLeastOnce()).save(any());
    verify(productVariantRepo, atLeastOnce()).save(any());
    verify(variantOptionMapRepo, atLeastOnce()).save(any());
  }

  @Test
  @DisplayName("thumbnailKey가 없으면 thumbnail은 null이다")
  void createProduct_noThumbnail_thumbnailIsNull() {
    ProductCreateRequest request = noOptionRequest();

    ProductCreateResponse response = productCreateService.create(AUTH_ID, request);

    assertThat(response.thumbnail()).isNull();
    verify(imageService, never()).validateAndGetProductUrl(any());
  }

  @Test
  @DisplayName("thumbnailKey가 있으면 products/ URL로 변환된다")
  void createProduct_withThumbnail_urlConverted() {
    ProductCreateRequest request = new ProductCreateRequest(
        "썸네일 상품", 5L,
        "temp/550e8400-e29b-41d4-a716-446655440000.jpg",
        10000, null, null, 100, null, null, null
    );

    ProductCreateResponse response = productCreateService.create(AUTH_ID, request);

    assertThat(response.thumbnail()).isEqualTo(
        "http://localhost:9000/wonkaotalk/products/test.jpg");
  }

  @Test
  @DisplayName("detail이 있으면 ProductDetail이 저장된다")
  void createProduct_withDetail_detailSaved() {
    ProductCreateRequest request = new ProductCreateRequest(
        "상세 상품", 5L, null, 10000, null,
        "<p>상품 설명입니다.</p>",
        100, null, null, null
    );

    productCreateService.create(AUTH_ID, request);

    verify(productDetailRepo).save(any());
  }

  @Test
  @DisplayName("detail이 없으면 ProductDetail은 저장되지 않는다")
  void createProduct_withoutDetail_detailNotSaved() {
    productCreateService.create(AUTH_ID, noOptionRequest());

    verify(productDetailRepo, never()).save(any());
  }

  // ── discountedPrice 계산 ───────────────────────────────────────────────────────

  @ParameterizedTest(name = "price={0}, discountRate={1} → discountedPrice={2}")
  @CsvSource({
      "59000, 20, 47200",
      "10000, 30, 7000",
      "15000, 0,  15000",
      "10000, 100, 0",
      "3333,  10, 3000",
  })
  @DisplayName("discountedPrice가 올바르게 계산된다")
  void discountedPrice_calculatedCorrectly(int price, int discountRate, int expected) {
    ProductCreateRequest request = new ProductCreateRequest(
        "할인 상품", 5L, null, price, discountRate, null, 100, null, null, null
    );

    ProductCreateResponse response = productCreateService.create(AUTH_ID, request);

    assertThat(response.discountedPrice()).isEqualTo(expected);
  }

  @Test
  @DisplayName("discountRate가 없으면 discountedPrice는 price와 같다")
  void discountedPrice_equalsPrice_whenNoDiscountRate() {
    ProductCreateRequest request = noOptionRequest();

    ProductCreateResponse response = productCreateService.create(AUTH_ID, request);

    assertThat(response.discountedPrice()).isEqualTo(response.price());
  }

  // ── 조회 실패 ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("판매자를 찾을 수 없으면 SELLER_NOT_FOUND를 던진다")
  void throwsException_whenSellerNotFound() {
    when(sellerRepo.findByAuthId(anyLong())).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, noOptionRequest()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SELLER_NOT_FOUND);
  }

  @Test
  @DisplayName("스토어를 찾을 수 없으면 PROD_STORE_NOT_FOUND를 던진다")
  void throwsException_whenStoreNotFound() {
    when(storeRepo.findBySeller(any())).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, noOptionRequest()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_STORE_NOT_FOUND);
  }

  @Test
  @DisplayName("카테고리를 찾을 수 없으면 PROD_CATEGORY_NOT_FOUND를 던진다")
  void throwsException_whenCategoryNotFound() {
    when(categoryRepo.findById(anyLong())).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, noOptionRequest()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_CATEGORY_NOT_FOUND);
  }

  // ── 가격/할인율 검증 ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("가격이 음수면 PROD_INVALID_PRICE를 던진다")
  void throwsException_whenPriceIsNegative() {
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, -1, null, null, 100, null, null, null
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_PRICE);
  }

  @ParameterizedTest(name = "discountRate={0}")
  @CsvSource({"-1", "101"})
  @DisplayName("할인율이 0~100 범위를 벗어나면 PROD_INVALID_DISCOUNT_RATE를 던진다")
  void throwsException_whenDiscountRateOutOfRange(int discountRate) {
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 10000, discountRate, null, 100, null, null, null
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_DISCOUNT_RATE);
  }

  // ── 재고 이력 ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("옵션 없는 상품 등록 시 재고 이력이 1건 저장된다")
  void createProduct_noOption_stockHistorySaved() {
    productCreateService.create(AUTH_ID, noOptionRequest());

    verify(stockHistoryRepo).save(any());
  }

  @Test
  @DisplayName("옵션 있는 상품 등록 시 variant 수만큼 재고 이력이 저장된다")
  void createProduct_withOption_stockHistorySavedPerVariant() {
    productCreateService.create(AUTH_ID, withOptionRequest());

    verify(stockHistoryRepo, atLeastOnce()).save(any());
  }

  // ── 재고 검증 ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("옵션 없는 상품에서 stock이 없으면 PROD_INVALID_QUANTITY를 던진다")
  void throwsException_whenNoOptionAndNoStock() {
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 10000, null, null, null, null, null, null
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_QUANTITY);
  }

  @Test
  @DisplayName("stock이 0이면 PROD_INVALID_QUANTITY를 던진다")
  void throwsException_whenStockIsZero() {
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 10000, null, null, 0, null, null, null
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_QUANTITY);
  }

  @Test
  @DisplayName("옵션 있는 상품에서 variant의 stock이 null이면 PROD_INVALID_QUANTITY를 던진다")
  void throwsException_whenVariantStockIsNull() {
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 15000, null, null, null, null,
        colorGroups(),
        List.of(new VariantRequest(List.of("화이트"), null))
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_QUANTITY);
  }

  @Test
  @DisplayName("옵션 있는 상품에서 variant의 stock이 0이면 PROD_INVALID_QUANTITY를 던진다")
  void throwsException_whenVariantStockIsZero() {
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 15000, null, null, null, null,
        colorGroups(),
        List.of(new VariantRequest(List.of("화이트"), 0))
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_QUANTITY);
  }

  // ── 옵션/variant 일관성 검증 ───────────────────────────────────────────────────

  @Test
  @DisplayName("optionGroups가 있는데 stock도 함께 전달하면 PROD_INVALID_STOCK_OPTION을 던진다")
  void throwsException_whenOptionGroupsAndStockBothPresent() {
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 15000, null, null, 100,
        null, colorGroups(), colorVariants()
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_STOCK_OPTION);
  }

  @Test
  @DisplayName("optionGroups만 있고 variants가 없으면 PROD_MISMATCH_VARIANT_OPTION을 던진다")
  void throwsException_whenOnlyOptionGroupsPresent() {
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 15000, null, null, null,
        null, colorGroups(), null
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_MISMATCH_VARIANT_OPTION);
  }

  @Test
  @DisplayName("variants만 있고 optionGroups가 없으면 PROD_MISMATCH_VARIANT_OPTION을 던진다")
  void throwsException_whenOnlyVariantsPresent() {
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 15000, null, null, null,
        null, null, colorVariants()
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_MISMATCH_VARIANT_OPTION);
  }

  @Test
  @DisplayName("같은 상품 내 optionGroup 이름이 중복되면 PROD_MISMATCH_VARIANT_OPTION을 던진다")
  void throwsException_whenDuplicateGroupName() {
    List<OptionGroupRequest> duplicateGroups = List.of(
        new OptionGroupRequest("색상", 1, List.of(new OptionRequest("화이트"))),
        new OptionGroupRequest("색상", 2, List.of(new OptionRequest("S")))
    );
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 15000, null, null, null, null,
        duplicateGroups,
        List.of(new VariantRequest(List.of("화이트", "S"), 10))
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_MISMATCH_VARIANT_OPTION);
  }

  @Test
  @DisplayName("같은 그룹 내 옵션 이름이 중복되면 PROD_MISMATCH_VARIANT_OPTION을 던진다")
  void throwsException_whenDuplicateOptionNameInGroup() {
    List<OptionGroupRequest> groups = List.of(
        new OptionGroupRequest("색상", 1, List.of(
            new OptionRequest("화이트"),
            new OptionRequest("화이트")
        ))
    );
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 15000, null, null, null, null,
        groups,
        List.of(new VariantRequest(List.of("화이트"), 10))
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_MISMATCH_VARIANT_OPTION);
  }

  @Test
  @DisplayName("variant의 optionNames 개수가 optionGroups 개수와 다르면 PROD_MISMATCH_VARIANT_OPTION을 던진다")
  void throwsException_whenOptionNamesCountMismatch() {
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 15000, null, null, null, null,
        colorGroups(),
        List.of(new VariantRequest(List.of("화이트", "여분옵션"), 10))
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_MISMATCH_VARIANT_OPTION);
  }

  @Test
  @DisplayName("variant의 optionName이 해당 그룹에 없으면 PROD_MISMATCH_VARIANT_OPTION을 던진다")
  void throwsException_whenOptionNameNotInGroup() {
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 15000, null, null, null, null,
        colorGroups(),
        List.of(new VariantRequest(List.of("존재하지않는색"), 10))
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_MISMATCH_VARIANT_OPTION);
  }

  @Test
  @DisplayName("동일한 optionNames 조합의 variant가 중복이면 PROD_MISMATCH_VARIANT_OPTION을 던진다")
  void throwsException_whenDuplicateVariantCombination() {
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 15000, null, null, null, null,
        colorGroups(),
        List.of(
            new VariantRequest(List.of("화이트"), 50),
            new VariantRequest(List.of("화이트"), 30)
        )
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_MISMATCH_VARIANT_OPTION);
  }

  // ── sortOrder 검증 ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("이미지 sortOrder가 중복이면 PROD_DUPLICATE_SORT_ORDER를 던진다")
  void throwsException_whenDuplicateImageSortOrder() {
    List<ImageRequest> images = List.of(
        new ImageRequest("temp/550e8400-e29b-41d4-a716-446655440000.jpg", 1),
        new ImageRequest("temp/661f9511-f30c-52e5-b827-557766551111.jpg", 1)
    );
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 10000, null, null, 100, images, null, null
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_DUPLICATE_SORT_ORDER);
  }

  @Test
  @DisplayName("optionGroup sortOrder가 중복이면 PROD_DUPLICATE_SORT_ORDER를 던진다")
  void throwsException_whenDuplicateGroupSortOrder() {
    List<OptionGroupRequest> groups = List.of(
        new OptionGroupRequest("색상", 1, List.of(new OptionRequest("화이트"))),
        new OptionGroupRequest("사이즈", 1, List.of(new OptionRequest("M")))
    );
    ProductCreateRequest request = new ProductCreateRequest(
        "상품", 5L, null, 15000, null, null, null, null,
        groups,
        List.of(new VariantRequest(List.of("화이트", "M"), 10))
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productCreateService.create(AUTH_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_DUPLICATE_SORT_ORDER);
  }

  // ── 헬퍼 ──────────────────────────────────────────────────────────────────────

  private ProductCreateRequest noOptionRequest() {
    return new ProductCreateRequest(
        "테스트 상품", 5L, null, 10000, null, null, 100, null, null, null
    );
  }

  private ProductCreateRequest withOptionRequest() {
    return new ProductCreateRequest(
        "옵션 상품", 5L, null, 15000, null, null, null, null,
        colorGroups(), colorVariants()
    );
  }

  private List<OptionGroupRequest> colorGroups() {
    return List.of(
        new OptionGroupRequest("색상", 1, List.of(
            new OptionRequest("화이트"),
            new OptionRequest("블랙")
        ))
    );
  }

  private List<VariantRequest> colorVariants() {
    return List.of(
        new VariantRequest(List.of("화이트"), 50),
        new VariantRequest(List.of("블랙"), 30)
    );
  }
}
