package com.example.WonkaoTalk.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.image.service.ImageService;
import com.example.WonkaoTalk.domain.product.dto.ProductEditFormResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductUpdateRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductUpdateRequest.ImageRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductUpdateResponse;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.DeletedProductImage;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductDetail;
import com.example.WonkaoTalk.domain.product.entity.ProductImage;
import com.example.WonkaoTalk.domain.product.entity.ProductOption;
import com.example.WonkaoTalk.domain.product.entity.ProductOptionGroup;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.event.ProductCreatedEvent;
import com.example.WonkaoTalk.domain.product.repo.CategoryRepo;
import com.example.WonkaoTalk.domain.product.repo.DeletedProductImageRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductDetailRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductImageRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionGroupRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductUpdateServiceTest {

  private static final Long AUTH_ID = 1L;
  private static final Long PRODUCT_ID = 101L;
  private static final Long STORE_ID = 7L;

  @Mock
  private SellerRepo sellerRepo;
  @Mock
  private StoreRepo storeRepo;
  @Mock
  private ProductRepo productRepo;
  @Mock
  private ProductDetailRepo productDetailRepo;
  @Mock
  private ProductImageRepo productImageRepo;
  @Mock
  private DeletedProductImageRepo deletedProductImageRepo;
  @Mock
  private ProductOptionGroupRepo productOptionGroupRepo;
  @Mock
  private ProductOptionRepo productOptionRepo;
  @Mock
  private ProductVariantRepo productVariantRepo;
  @Mock
  private CategoryRepo categoryRepo;
  @Mock
  private ImageService imageService;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private ProductUpdateService productUpdateService;

  private Store store;
  private Product product;
  private Category category;

  @BeforeEach
  void setUp() {
    Seller seller = mock(Seller.class);
    store = mock(Store.class);
    product = mock(Product.class);
    category = mock(Category.class);

    when(store.getId()).thenReturn(STORE_ID);
    when(category.getId()).thenReturn(5L);

    when(sellerRepo.findByAuthId(AUTH_ID)).thenReturn(Optional.of(seller));
    when(storeRepo.findBySeller(seller)).thenReturn(Optional.of(store));
    when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    when(product.getId()).thenReturn(PRODUCT_ID);
    when(product.getStore()).thenReturn(store);
    when(product.getDeletedAt()).thenReturn(null);
    when(product.getName()).thenReturn("기존 상품명");
    when(product.getThumbnail()).thenReturn(null);
    when(product.getPrice()).thenReturn(10000);
    when(product.getDiscountRate()).thenReturn(0);
    when(product.getDiscountedPrice()).thenReturn(10000);
    when(product.getStatus()).thenReturn(SaleStatus.ON_SALE);
    when(product.getCategory()).thenReturn(category);
    when(product.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 5, 24, 10, 0));

    when(productRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(deletedProductImageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(productImageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(productDetailRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    when(productImageRepo.findByProductIdOrderBySortOrderAsc(PRODUCT_ID)).thenReturn(List.of());
    when(productDetailRepo.findFirstByProductId(PRODUCT_ID)).thenReturn(Optional.empty());
    when(productOptionGroupRepo.findByProductId(PRODUCT_ID)).thenReturn(List.of());
    when(productVariantRepo.findByProductId(PRODUCT_ID)).thenReturn(List.of());

    when(imageService.validateAndGetProductUrl(anyString()))
        .thenReturn("http://localhost:9000/wonkaotalk/products/test.jpg");
  }

  // ── getEditForm 조회 실패 ────────────────────────────────────────────────────

  @Test
  @DisplayName("판매자를 찾을 수 없으면 SELLER_NOT_FOUND를 던진다")
  void getEditForm_throwsException_whenSellerNotFound() {
    when(sellerRepo.findByAuthId(anyLong())).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.getEditForm(AUTH_ID, PRODUCT_ID));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SELLER_NOT_FOUND);
  }

  @Test
  @DisplayName("스토어를 찾을 수 없으면 PROD_STORE_NOT_FOUND를 던진다")
  void getEditForm_throwsException_whenStoreNotFound() {
    when(storeRepo.findBySeller(any())).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.getEditForm(AUTH_ID, PRODUCT_ID));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_STORE_NOT_FOUND);
  }

  @Test
  @DisplayName("상품을 찾을 수 없으면 PROD_NOT_FOUND를 던진다")
  void getEditForm_throwsException_whenProductNotFound() {
    when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.getEditForm(AUTH_ID, PRODUCT_ID));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_NOT_FOUND);
  }

  @Test
  @DisplayName("논리 삭제된 상품이면 PROD_DELETED를 던진다")
  void getEditForm_throwsException_whenProductIsDeleted() {
    when(product.getDeletedAt()).thenReturn(LocalDateTime.now());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.getEditForm(AUTH_ID, PRODUCT_ID));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_DELETED);
  }

  @Test
  @DisplayName("다른 스토어의 상품이면 FORBIDDEN을 던진다")
  void getEditForm_throwsException_whenProductBelongsToOtherStore() {
    Store otherStore = mock(Store.class);
    when(otherStore.getId()).thenReturn(999L);
    when(product.getStore()).thenReturn(otherStore);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.getEditForm(AUTH_ID, PRODUCT_ID));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  // ── getEditForm 성공 ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("수정 폼 조회 시 이미지 목록이 포함된다")
  void getEditForm_includesImages() {
    ProductImage img = mockProductImage(10L, "http://example.com/img.jpg", 1);
    when(productImageRepo.findByProductIdOrderBySortOrderAsc(PRODUCT_ID)).thenReturn(List.of(img));

    ProductEditFormResponse response = productUpdateService.getEditForm(AUTH_ID, PRODUCT_ID);

    assertThat(response.getImages()).hasSize(1);
    assertThat(response.getImages().get(0).getImageId()).isEqualTo(10L);
    assertThat(response.getImages().get(0).getUrl()).isEqualTo("http://example.com/img.jpg");
    assertThat(response.getImages().get(0).getSortOrder()).isEqualTo(1);
  }

  @Test
  @DisplayName("수정 폼 조회 시 상세 설명이 포함된다")
  void getEditForm_includesDetail() {
    ProductDetail detail = mock(ProductDetail.class);
    when(detail.getContent()).thenReturn("<p>상품 설명</p>");
    when(productDetailRepo.findFirstByProductId(PRODUCT_ID)).thenReturn(Optional.of(detail));

    ProductEditFormResponse response = productUpdateService.getEditForm(AUTH_ID, PRODUCT_ID);

    assertThat(response.getDetail()).isEqualTo("<p>상품 설명</p>");
  }

  @Test
  @DisplayName("수정 폼 조회 시 옵션 그룹과 옵션 목록이 포함된다")
  void getEditForm_includesOptionGroupsAndOptions() {
    ProductOptionGroup group = mockOptionGroup(20L, "색상", 1);
    ProductOption option = mockProductOption(30L, "화이트", group);
    when(productOptionGroupRepo.findByProductId(PRODUCT_ID)).thenReturn(List.of(group));
    when(productOptionRepo.findByProductOptionGroupIdIn(List.of(20L))).thenReturn(List.of(option));

    ProductEditFormResponse response = productUpdateService.getEditForm(AUTH_ID, PRODUCT_ID);

    assertThat(response.getOptionGroups()).hasSize(1);
    assertThat(response.getOptionGroups().get(0).getOptionGroupId()).isEqualTo(20L);
    assertThat(response.getOptionGroups().get(0).getName()).isEqualTo("색상");
    assertThat(response.getOptionGroups().get(0).getOptions()).hasSize(1);
    assertThat(response.getOptionGroups().get(0).getOptions().get(0).getName()).isEqualTo("화이트");
  }

  @Test
  @DisplayName("수정 폼 조회 시 variant 목록이 포함된다")
  void getEditForm_includesVariants() {
    ProductVariant variant = mockVariant(40L, "화이트", 50, SaleStatus.ON_SALE);
    when(productVariantRepo.findByProductId(PRODUCT_ID)).thenReturn(List.of(variant));

    ProductEditFormResponse response = productUpdateService.getEditForm(AUTH_ID, PRODUCT_ID);

    assertThat(response.getVariants()).hasSize(1);
    assertThat(response.getVariants().get(0).getVariantId()).isEqualTo(40L);
    assertThat(response.getVariants().get(0).getVariantName()).isEqualTo("화이트");
    assertThat(response.getVariants().get(0).getStock()).isEqualTo(50);
    assertThat(response.getVariants().get(0).getStatus()).isEqualTo("ON_SALE");
  }

  @Test
  @DisplayName("상세 설명이 없으면 detail은 null이다")
  void getEditForm_detailIsNull_whenNoDetail() {
    when(productDetailRepo.findFirstByProductId(PRODUCT_ID)).thenReturn(Optional.empty());

    ProductEditFormResponse response = productUpdateService.getEditForm(AUTH_ID, PRODUCT_ID);

    assertThat(response.getDetail()).isNull();
  }

  // ── update 조회 실패 ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("update: 판매자를 찾을 수 없으면 SELLER_NOT_FOUND를 던진다")
  void update_throwsException_whenSellerNotFound() {
    when(sellerRepo.findByAuthId(anyLong())).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.update(AUTH_ID, PRODUCT_ID, emptyRequest()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SELLER_NOT_FOUND);
  }

  @Test
  @DisplayName("update: 상품을 찾을 수 없으면 PROD_NOT_FOUND를 던진다")
  void update_throwsException_whenProductNotFound() {
    when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.update(AUTH_ID, PRODUCT_ID, emptyRequest()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_NOT_FOUND);
  }

  @Test
  @DisplayName("update: 다른 스토어의 상품이면 FORBIDDEN을 던진다")
  void update_throwsException_whenProductBelongsToOtherStore() {
    Store otherStore = mock(Store.class);
    when(otherStore.getId()).thenReturn(999L);
    when(product.getStore()).thenReturn(otherStore);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.update(AUTH_ID, PRODUCT_ID, emptyRequest()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  // ── update 입력값 검증 ───────────────────────────────────────────────────────

  @Test
  @DisplayName("가격이 음수면 PROD_INVALID_PRICE를 던진다")
  void update_throwsException_whenPriceIsNegative() {
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, -1, null, null, null, null);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.update(AUTH_ID, PRODUCT_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_PRICE);
  }

  @Test
  @DisplayName("할인율이 음수면 PROD_INVALID_DISCOUNT_RATE를 던진다")
  void update_throwsException_whenDiscountRateIsNegative() {
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, -1, null, null, null);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.update(AUTH_ID, PRODUCT_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_DISCOUNT_RATE);
  }

  @Test
  @DisplayName("할인율이 100을 초과하면 PROD_INVALID_DISCOUNT_RATE를 던진다")
  void update_throwsException_whenDiscountRateExceeds100() {
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, 101, null, null, null);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.update(AUTH_ID, PRODUCT_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_DISCOUNT_RATE);
  }

  @Test
  @DisplayName("잘못된 상태값이면 BAD_REQUEST를 던진다")
  void update_throwsException_whenStatusIsInvalid() {
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, null, null, "INVALID_STATUS", null);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.update(AUTH_ID, PRODUCT_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  @DisplayName("이미지 요청에 imageId와 objectKey가 모두 있으면 BAD_REQUEST를 던진다")
  void update_throwsException_whenImageHasBothIdAndKey() {
    List<ImageRequest> images = List.of(new ImageRequest(1L, "temp/key.jpg", 1));
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, null, null, null, images);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.update(AUTH_ID, PRODUCT_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  @DisplayName("이미지 요청에 imageId와 objectKey가 모두 없으면 BAD_REQUEST를 던진다")
  void update_throwsException_whenImageHasNeitherIdNorKey() {
    List<ImageRequest> images = List.of(new ImageRequest(null, null, 1));
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, null, null, null, images);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.update(AUTH_ID, PRODUCT_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  // ── update 카테고리 ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("존재하지 않는 카테고리 ID면 PROD_CATEGORY_NOT_FOUND를 던진다")
  void update_throwsException_whenCategoryNotFound() {
    when(categoryRepo.findById(anyLong())).thenReturn(Optional.empty());
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, 99L, null, null, null, null, null, null);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.update(AUTH_ID, PRODUCT_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_CATEGORY_NOT_FOUND);
  }

  @Test
  @DisplayName("categoryId가 null이면 카테고리를 조회하지 않는다")
  void update_doesNotQueryCategory_whenCategoryIdIsNull() {
    productUpdateService.update(AUTH_ID, PRODUCT_ID, emptyRequest());

    verify(categoryRepo, never()).findById(anyLong());
  }

  // ── update 썸네일 처리 ───────────────────────────────────────────────────────

  @Test
  @DisplayName("thumbnailKey가 있으면 이미지 URL로 변환된다")
  void update_convertsThumbnailKey_toProductUrl() {
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, "temp/new-thumb.jpg", null, null, null, null, null);

    ProductUpdateResponse response = productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(imageService).validateAndGetProductUrl("temp/new-thumb.jpg");
  }

  @Test
  @DisplayName("썸네일 교체 시 기존 썸네일 URL을 deleted_product_images에 기록한다")
  void update_recordsOldThumbnailUrl_whenReplacingThumbnail() {
    when(product.getThumbnail()).thenReturn("http://example.com/old-thumb.jpg");
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, "temp/new-thumb.jpg", null, null, null, null, null);

    productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(deletedProductImageRepo).save(any(DeletedProductImage.class));
  }

  @Test
  @DisplayName("기존 썸네일이 없으면 deleted_product_images에 기록하지 않는다")
  void update_doesNotRecordDeletedUrl_whenNoExistingThumbnail() {
    when(product.getThumbnail()).thenReturn(null);
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, "temp/new-thumb.jpg", null, null, null, null, null);

    productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(deletedProductImageRepo, never()).save(any());
  }

  @Test
  @DisplayName("thumbnailKey가 없으면 thumbnailUrl을 변경하지 않는다")
  void update_doesNotChangeThumbnail_whenKeyIsNull() {
    ProductUpdateRequest request = emptyRequest();

    productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(imageService, never()).validateAndGetProductUrl(any());
  }

  // ── update 이미지 처리 ───────────────────────────────────────────────────────

  @Test
  @DisplayName("images가 빈 리스트이면 기존 이미지를 전부 삭제한다")
  void update_deletesAllImages_whenImagesIsEmpty() {
    ProductImage existingImg = mockProductImage(10L, "http://example.com/img.jpg", 1);
    when(productImageRepo.findByProductIdOrderBySortOrderAsc(PRODUCT_ID))
        .thenReturn(List.of(existingImg));

    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, null, null, null, List.of());

    productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(productImageRepo).deleteAll(List.of(existingImg));
    verify(deletedProductImageRepo).save(any(DeletedProductImage.class));
  }

  @Test
  @DisplayName("기존 이미지 ID를 포함하면 해당 이미지는 삭제되지 않는다")
  void update_keepsExistingImage_whenImageIdProvided() {
    ProductImage existingImg = mockProductImage(10L, "http://example.com/img.jpg", 1);
    when(productImageRepo.findByProductIdOrderBySortOrderAsc(PRODUCT_ID))
        .thenReturn(List.of(existingImg));
    when(productImageRepo.findByProductIdAndIdIn(PRODUCT_ID, List.of(10L)))
        .thenReturn(List.of(existingImg));

    List<ImageRequest> images = List.of(new ImageRequest(10L, null, 2));
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, null, null, null, images);

    productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(productImageRepo).deleteAll(List.of());
    verify(productImageRepo, never()).save(any());
  }

  @Test
  @DisplayName("기존 이미지 ID를 제외하면 해당 이미지는 삭제된다")
  void update_deletesImage_whenImageIdNotInRequest() {
    ProductImage img1 = mockProductImage(10L, "http://example.com/img1.jpg", 1);
    ProductImage img2 = mockProductImage(11L, "http://example.com/img2.jpg", 2);
    when(productImageRepo.findByProductIdOrderBySortOrderAsc(PRODUCT_ID))
        .thenReturn(List.of(img1, img2));
    when(productImageRepo.findByProductIdAndIdIn(PRODUCT_ID, List.of(10L)))
        .thenReturn(List.of(img1));

    List<ImageRequest> images = List.of(new ImageRequest(10L, null, 1));
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, null, null, null, images);

    productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(productImageRepo).deleteAll(List.of(img2));
  }

  @Test
  @DisplayName("새 이미지 objectKey를 전달하면 새 이미지가 저장된다")
  void update_savesNewImage_whenObjectKeyProvided() {
    List<ImageRequest> images = List.of(new ImageRequest(null, "temp/new.jpg", 1));
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, null, null, null, images);

    productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(imageService).validateAndGetProductUrl("temp/new.jpg");
    verify(productImageRepo).save(any(ProductImage.class));
  }

  @Test
  @DisplayName("이미지 sortOrder가 중복이면 PROD_DUPLICATE_SORT_ORDER를 던진다")
  void update_throwsException_whenDuplicateImageSortOrder() {
    List<ImageRequest> images = List.of(
        new ImageRequest(null, "temp/a.jpg", 1),
        new ImageRequest(null, "temp/b.jpg", 1)
    );
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, null, null, null, images);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.update(AUTH_ID, PRODUCT_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_DUPLICATE_SORT_ORDER);
  }

  @Test
  @DisplayName("요청한 이미지 ID가 해당 상품에 속하지 않으면 PROD_INVALID_IMAGE_ID를 던진다")
  void update_throwsException_whenImageIdNotBelongToProduct() {
    ProductImage existingImg = mockProductImage(10L, "http://example.com/img.jpg", 1);
    when(productImageRepo.findByProductIdOrderBySortOrderAsc(PRODUCT_ID))
        .thenReturn(List.of(existingImg));
    when(productImageRepo.findByProductIdAndIdIn(PRODUCT_ID, List.of(999L)))
        .thenReturn(List.of());

    List<ImageRequest> images = List.of(new ImageRequest(999L, null, 1));
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, null, null, null, images);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productUpdateService.update(AUTH_ID, PRODUCT_ID, request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_IMAGE_ID);
  }

  @Test
  @DisplayName("images가 null이면 이미지를 처리하지 않는다")
  void update_doesNotProcessImages_whenImagesIsNull() {
    ProductUpdateRequest request = emptyRequest();

    productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(productImageRepo, never()).deleteAll(any());
    verify(productImageRepo, never()).save(any());
  }

  // ── update 상세 설명 처리 ────────────────────────────────────────────────────

  @Test
  @DisplayName("detail이 null이면 상세 설명을 처리하지 않는다")
  void update_doesNotProcessDetail_whenDetailIsNull() {
    productUpdateService.update(AUTH_ID, PRODUCT_ID, emptyRequest());

    verify(productDetailRepo, never()).save(any());
    verify(productDetailRepo, never()).delete(any());
  }

  @Test
  @DisplayName("detail이 빈 문자열이면 기존 상세 설명을 삭제한다")
  void update_deletesDetail_whenDetailIsEmpty() {
    ProductDetail existingDetail = mock(ProductDetail.class);
    when(productDetailRepo.findFirstByProductId(PRODUCT_ID))
        .thenReturn(Optional.of(existingDetail));

    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, null, "", null, null);

    productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(productDetailRepo).delete(existingDetail);
  }

  @Test
  @DisplayName("detail이 빈 문자열이고 기존 상세가 없으면 삭제를 시도하지 않는다")
  void update_doesNotDelete_whenDetailIsEmptyAndNoExistingDetail() {
    when(productDetailRepo.findFirstByProductId(PRODUCT_ID)).thenReturn(Optional.empty());
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, null, "", null, null);

    productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(productDetailRepo, never()).delete(any());
  }

  @Test
  @DisplayName("detail이 있고 기존 상세가 없으면 새로 저장한다")
  void update_savesNewDetail_whenDetailAndNoExistingDetail() {
    when(productDetailRepo.findFirstByProductId(PRODUCT_ID)).thenReturn(Optional.empty());
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, null, "<p>새 설명</p>", null, null);

    productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(productDetailRepo).save(any(ProductDetail.class));
  }

  @Test
  @DisplayName("detail이 있고 기존 상세가 있으면 내용을 업데이트한다")
  void update_updatesExistingDetail_whenDetailAndExistingDetail() {
    ProductDetail existingDetail = mock(ProductDetail.class);
    when(productDetailRepo.findFirstByProductId(PRODUCT_ID))
        .thenReturn(Optional.of(existingDetail));
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, null, "<p>수정된 설명</p>", null, null);

    productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(existingDetail).updateContent(any());
    verify(productDetailRepo, never()).save(any());
  }

  // ── update 성공 응답 ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("수정 성공 시 응답에 상품 ID가 포함된다")
  void update_returnsProductId_onSuccess() {
    ProductUpdateResponse response = productUpdateService.update(AUTH_ID, PRODUCT_ID,
        emptyRequest());

    assertThat(response.productId()).isEqualTo(PRODUCT_ID);
  }

  @Test
  @DisplayName("수정 성공 시 이벤트가 발행되지 않는다 - S3 이동 대상이 없을 때")
  void update_doesNotPublishEvent_whenNoObjectKeysToMove() {
    productUpdateService.update(AUTH_ID, PRODUCT_ID, emptyRequest());

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("thumbnailKey가 있으면 이벤트가 발행된다")
  void update_publishesEvent_whenThumbnailKeyProvided() {
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, "temp/new-thumb.jpg", null, null, null, null, null);

    productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(eventPublisher).publishEvent(any(ProductCreatedEvent.class));
  }

  @Test
  @DisplayName("새 이미지 objectKey가 있으면 이벤트가 발행된다")
  void update_publishesEvent_whenNewImageObjectKeyProvided() {
    List<ImageRequest> images = List.of(new ImageRequest(null, "temp/new.jpg", 1));
    ProductUpdateRequest request = new ProductUpdateRequest(
        null, null, null, null, null, null, null, images);

    productUpdateService.update(AUTH_ID, PRODUCT_ID, request);

    verify(eventPublisher).publishEvent(any(ProductCreatedEvent.class));
  }

  // ── 헬퍼 ────────────────────────────────────────────────────────────────────

  private ProductUpdateRequest emptyRequest() {
    return new ProductUpdateRequest(null, null, null, null, null, null, null, null);
  }

  private ProductImage mockProductImage(Long id, String url, Integer sortOrder) {
    ProductImage img = mock(ProductImage.class);
    when(img.getId()).thenReturn(id);
    when(img.getUrl()).thenReturn(url);
    when(img.getSortOrder()).thenReturn(sortOrder);
    return img;
  }

  private ProductOptionGroup mockOptionGroup(Long id, String name, Integer sortOrder) {
    ProductOptionGroup group = mock(ProductOptionGroup.class);
    when(group.getId()).thenReturn(id);
    when(group.getName()).thenReturn(name);
    when(group.getSortOrder()).thenReturn(sortOrder);
    return group;
  }

  private ProductOption mockProductOption(Long id, String name, ProductOptionGroup group) {
    ProductOption option = mock(ProductOption.class);
    when(option.getId()).thenReturn(id);
    when(option.getName()).thenReturn(name);
    when(option.getProductOptionGroup()).thenReturn(group);
    return option;
  }

  private ProductVariant mockVariant(Long id, String name, int stock, SaleStatus status) {
    ProductVariant variant = mock(ProductVariant.class);
    when(variant.getId()).thenReturn(id);
    when(variant.getName()).thenReturn(name);
    when(variant.getStock()).thenReturn(stock);
    when(variant.getStatus()).thenReturn(status);
    return variant;
  }
}
