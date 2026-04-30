package com.example.WonkaoTalk.domain.product.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.config.TestContainerConfig;
import com.example.WonkaoTalk.domain.product.dto.CartAddRequest;
import com.example.WonkaoTalk.domain.product.dto.CartAddResponse;
import com.example.WonkaoTalk.domain.product.dto.CartDeleteResponse;
import com.example.WonkaoTalk.domain.product.dto.CartOptionUpdateRequest;
import com.example.WonkaoTalk.domain.product.dto.CartOptionUpdateResponse;
import com.example.WonkaoTalk.domain.product.dto.CartQuantityUpdateRequest;
import com.example.WonkaoTalk.domain.product.dto.CartQuantityUpdateResponse;
import com.example.WonkaoTalk.domain.product.dto.CartResponse;
import com.example.WonkaoTalk.domain.product.entity.Cart;
import com.example.WonkaoTalk.domain.product.entity.CartItem;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.service.CartService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestContainerConfig.class)
@Transactional
class CartServiceIntegrationTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private CartService cartService;

  private Category category;
  private Product productA;
  private Product productB;
  private ProductVariant variantA1;
  private ProductVariant variantA2;
  private ProductVariant variantB1;

  @BeforeEach
  void setUp() {
    category = saveCategory();
    productA = saveProduct(category, "상품A", 10000, 8000);
    productB = saveProduct(category, "상품B", 5000, 4000);
    variantA1 = saveVariant(productA, "옵션A-1", 10, SaleStatus.ON_SALE);
    variantA2 = saveVariant(productA, "옵션A-2", 5, SaleStatus.ON_SALE);
    variantB1 = saveVariant(productB, "옵션B-1", 20, SaleStatus.ON_SALE);
    em.flush();
    em.clear();
  }

  // ── getCart ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("장바구니가 없으면 빈 응답을 반환한다")
  void getCart_returnsEmpty_whenNoCartExists() {
    CartResponse response = cartService.getCart(999L);

    assertThat(response.getCartId()).isNull();
    assertThat(response.getCartItems()).isEmpty();
    assertThat(response.getSummary().getOriginalTotalAmount()).isEqualTo(0);
    assertThat(response.getSummary().getDiscountTotalAmount()).isEqualTo(0);
  }

  @Test
  @DisplayName("장바구니 목록 조회 시 CartItem 정보와 합계 금액이 정확하게 반환된다")
  void getCart_returnsCartItemsAndTotals_correctly() {
    Cart cart = saveCart(1L);
    saveCartItem(cart, variantA1, 2);
    saveCartItem(cart, variantB1, 3);
    em.flush();
    em.clear();

    CartResponse response = cartService.getCart(1L);

    assertThat(response.getCartId()).isEqualTo(cart.getId());
    assertThat(response.getCartItems()).hasSize(2);
    assertThat(response.getSummary().getOriginalTotalAmount()).isEqualTo(35000); // 10000*2 + 5000*3
    assertThat(response.getSummary().getDiscountTotalAmount()).isEqualTo(28000); // 8000*2 + 4000*3
  }

  @Test
  @DisplayName("getCart는 fetch join으로 CartItem의 variantName, price 등을 정상적으로 반환한다")
  void getCart_returnsCorrectVariantInfo_viaFetchJoin() {
    Cart cart = saveCart(1L);
    saveCartItem(cart, variantA1, 1);
    em.flush();
    em.clear();

    CartResponse response = cartService.getCart(1L);

    CartResponse.CartItemInfo item = response.getCartItems().get(0);
    assertThat(item.getVariantName()).isEqualTo("옵션A-1");
    assertThat(item.getPrice()).isEqualTo(10000);
    assertThat(item.getDiscountedPrice()).isEqualTo(8000);
    assertThat(item.getQuantity()).isEqualTo(1);
    assertThat(item.getStock()).isEqualTo(10);
  }

  // ── addToCart ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("장바구니가 없을 때 addToCart 호출 시 Cart가 자동 생성된다")
  void addToCart_createsCart_whenCartNotExists() {
    CartAddResponse response = cartService.addToCart(100L, addRequest(productA.getId(), variantA1.getId(), 2));

    assertThat(response.getCartItemId()).isNotNull();

    em.flush();
    em.clear();

    CartResponse cart = cartService.getCart(100L);
    assertThat(cart.getCartId()).isNotNull();
    assertThat(cart.getCartItems()).hasSize(1);
    assertThat(cart.getCartItems().get(0).getQuantity()).isEqualTo(2);
  }

  @Test
  @DisplayName("동일한 variant를 중복 추가하면 수량이 합산된다 (Upsert)")
  void addToCart_mergesQuantity_whenSameVariantAdded() {
    Cart cart = saveCart(1L);
    saveCartItem(cart, variantA1, 3);
    em.flush();
    em.clear();

    cartService.addToCart(1L, addRequest(productA.getId(), variantA1.getId(), 2));

    em.flush();
    em.clear();

    CartResponse response = cartService.getCart(1L);
    assertThat(response.getCartItems()).hasSize(1);
    assertThat(response.getCartItems().get(0).getQuantity()).isEqualTo(5); // 3 + 2
  }

  @Test
  @DisplayName("다른 variant를 추가하면 새 CartItem이 생성된다")
  void addToCart_addsNewItem_whenDifferentVariantAdded() {
    Cart cart = saveCart(1L);
    saveCartItem(cart, variantA1, 2);
    em.flush();
    em.clear();

    cartService.addToCart(1L, addRequest(productA.getId(), variantA2.getId(), 1));

    em.flush();
    em.clear();

    CartResponse response = cartService.getCart(1L);
    assertThat(response.getCartItems()).hasSize(2);
  }

  @Test
  @DisplayName("ON_SALE이 아닌 variant는 추가할 수 없다")
  void addToCart_throwsUnavailable_whenVariantNotOnSale() {
    ProductVariant stopped = saveVariant(productA, "판매중단옵션", 10, SaleStatus.STOP_SALE);
    em.flush();
    em.clear();

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.addToCart(1L, addRequest(productA.getId(), stopped.getId(), 1)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_VARIANT_UNAVAILABLE);
  }

  @Test
  @DisplayName("Upsert 시 합산 수량이 재고를 초과하면 예외를 던진다")
  void addToCart_throwsStockInsufficient_whenUpsertExceedsStock() {
    Cart cart = saveCart(1L);
    saveCartItem(cart, variantA2, 4); // variantA2 stock=5, 기존 4
    em.flush();
    em.clear();

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.addToCart(1L, addRequest(productA.getId(), variantA2.getId(), 2))); // 4+2=6 > 5

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_STOCK_INSUFFICIENT);
  }

  // ── updateCartItemQuantity ───────────────────────────────────────────────────

  @Test
  @DisplayName("수량 수정 시 Dirty Checking으로 실제 DB에 반영된다")
  void updateCartItemQuantity_persistsViaDirectyChecking() {
    Cart cart = saveCart(1L);
    CartItem item = saveCartItem(cart, variantA1, 2);
    em.flush();
    em.clear();

    cartService.updateCartItemQuantity(1L, item.getId(), quantityUpdateRequest(5));

    em.flush();
    em.clear();

    CartResponse response = cartService.getCart(1L);
    assertThat(response.getCartItems().get(0).getQuantity()).isEqualTo(5);
  }

  @Test
  @DisplayName("수량 수정 후 전체 합계 금액이 재계산되어 반환된다")
  void updateCartItemQuantity_recalculatesTotals() {
    Cart cart = saveCart(1L);
    CartItem item = saveCartItem(cart, variantA1, 2);
    saveCartItem(cart, variantB1, 1);
    em.flush();
    em.clear();

    CartQuantityUpdateResponse response =
        cartService.updateCartItemQuantity(1L, item.getId(), quantityUpdateRequest(4));

    // variantA1: 10000*4, variantB1: 5000*1
    assertThat(response.getOriginalTotalAmount()).isEqualTo(45000);
    // variantA1: 8000*4, variantB1: 4000*1
    assertThat(response.getDiscountTotalAmount()).isEqualTo(36000);
  }

  @Test
  @DisplayName("타인의 CartItem 수량 수정 시 FORBIDDEN을 던진다")
  void updateCartItemQuantity_throwsForbidden_whenNotOwned() {
    Cart otherCart = saveCart(99L);
    CartItem otherItem = saveCartItem(otherCart, variantA1, 2);
    saveCart(1L);
    em.flush();
    em.clear();

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemQuantity(1L, otherItem.getId(), quantityUpdateRequest(3)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  // ── updateCartItemOption ────────────────────────────────────────────────────

  @Test
  @DisplayName("옵션 변경 시 variant가 실제로 변경된다 (Update 경로)")
  void updateCartItemOption_changesVariant_whenNoDuplicate() {
    Cart cart = saveCart(1L);
    CartItem item = saveCartItem(cart, variantA1, 2);
    em.flush();
    em.clear();

    CartOptionUpdateResponse response =
        cartService.updateCartItemOption(1L, item.getId(), optionUpdateRequest(variantA2.getId()));

    assertThat(response.isMerged()).isFalse();
    assertThat(response.getCartItem().getVariantId()).isEqualTo(variantA2.getId());

    em.flush();
    em.clear();

    CartResponse cartResponse = cartService.getCart(1L);
    assertThat(cartResponse.getCartItems()).hasSize(1);
    assertThat(cartResponse.getCartItems().get(0).getVariantId()).isEqualTo(variantA2.getId());
  }

  @Test
  @DisplayName("옵션 변경 대상 variant가 이미 장바구니에 있으면 수량이 합산되고 기존 아이템은 삭제된다 (Merge 경로)")
  void updateCartItemOption_merges_whenTargetVariantAlreadyInCart() {
    Cart cart = saveCart(1L);
    CartItem itemA1 = saveCartItem(cart, variantA1, 2);
    saveCartItem(cart, variantA2, 1);
    em.flush();
    em.clear();

    CartOptionUpdateResponse response =
        cartService.updateCartItemOption(1L, itemA1.getId(), optionUpdateRequest(variantA2.getId()));

    assertThat(response.isMerged()).isTrue();
    assertThat(response.getCartItem().getVariantId()).isEqualTo(variantA2.getId());
    assertThat(response.getCartItem().getQuantity()).isEqualTo(3); // 1 + 2

    em.flush();
    em.clear();

    CartResponse cartResponse = cartService.getCart(1L);
    assertThat(cartResponse.getCartItems()).hasSize(1);
    assertThat(cartResponse.getCartItems().get(0).getQuantity()).isEqualTo(3);
  }

  @Test
  @DisplayName("Merge 시 합산 수량이 재고를 초과하면 예외를 던진다")
  void updateCartItemOption_throwsStockInsufficient_whenMergedExceedsStock() {
    Cart cart = saveCart(1L);
    CartItem itemA1 = saveCartItem(cart, variantA1, 3);
    saveCartItem(cart, variantA2, 4); // variantA2 stock=5, 기존 4 + 변경 3 = 7 > 5
    em.flush();
    em.clear();

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemOption(1L, itemA1.getId(), optionUpdateRequest(variantA2.getId())));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_STOCK_INSUFFICIENT);
  }

  // ── deleteFromCart ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("isAllDelete=true이면 Cart의 모든 CartItem이 삭제된다")
  void deleteFromCart_deletesAllItems_whenIsAllDeleteTrue() {
    Cart cart = saveCart(1L);
    saveCartItem(cart, variantA1, 2);
    saveCartItem(cart, variantB1, 1);
    em.flush();
    em.clear();

    CartDeleteResponse response = cartService.deleteFromCart(1L, null, true);

    assertThat(response.getCartId()).isEqualTo(cart.getId());

    em.flush();
    em.clear();

    CartResponse cartResponse = cartService.getCart(1L);
    assertThat(cartResponse.getCartItems()).isEmpty();
  }

  @Test
  @DisplayName("선택 삭제 시 지정한 CartItem만 삭제된다")
  void deleteFromCart_deletesOnlySelectedItems() {
    Cart cart = saveCart(1L);
    CartItem item1 = saveCartItem(cart, variantA1, 2);
    CartItem item2 = saveCartItem(cart, variantB1, 1);
    em.flush();
    em.clear();

    cartService.deleteFromCart(1L, List.of(item1.getId()), false);

    em.flush();
    em.clear();

    CartResponse cartResponse = cartService.getCart(1L);
    assertThat(cartResponse.getCartItems()).hasSize(1);
    assertThat(cartResponse.getCartItems().get(0).getCartItemId()).isEqualTo(item2.getId());
  }

  @Test
  @DisplayName("타인의 CartItem을 삭제하려 하면 NOT_FOUND를 던진다")
  void deleteFromCart_throwsNotFound_whenItemBelongsToOther() {
    Cart otherCart = saveCart(99L);
    CartItem otherItem = saveCartItem(otherCart, variantA1, 2);
    saveCart(1L);
    em.flush();
    em.clear();

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.deleteFromCart(1L, List.of(otherItem.getId()), false));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  @DisplayName("존재하지 않는 CartItem ID를 삭제하려 하면 NOT_FOUND를 던진다")
  void deleteFromCart_throwsNotFound_whenItemNotExist() {
    saveCart(1L);
    em.flush();
    em.clear();

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.deleteFromCart(1L, List.of(99999L), false));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

  private Category saveCategory() {
    Category cat = new Category();
    ReflectionTestUtils.setField(cat, "name", "테스트카테고리");
    ReflectionTestUtils.setField(cat, "depth", 1);
    em.persist(cat);
    return cat;
  }

  private Product saveProduct(Category cat, String name, int price, int discountedPrice) {
    Product p = new Product();
    ReflectionTestUtils.setField(p, "storeId", 1L);
    ReflectionTestUtils.setField(p, "name", name);
    ReflectionTestUtils.setField(p, "category", cat);
    ReflectionTestUtils.setField(p, "price", price);
    ReflectionTestUtils.setField(p, "discountedPrice", discountedPrice);
    ReflectionTestUtils.setField(p, "likeCount", 0);
    ReflectionTestUtils.setField(p, "status", SaleStatus.ON_SALE);
    ReflectionTestUtils.setField(p, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(p, "updatedAt", LocalDateTime.now());
    em.persist(p);
    return p;
  }

  private ProductVariant saveVariant(Product product, String name, int stock, SaleStatus status) {
    ProductVariant variant = new ProductVariant();
    ReflectionTestUtils.setField(variant, "product", product);
    ReflectionTestUtils.setField(variant, "name", name);
    ReflectionTestUtils.setField(variant, "stock", stock);
    ReflectionTestUtils.setField(variant, "status", status);
    ReflectionTestUtils.setField(variant, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(variant, "updatedAt", LocalDateTime.now());
    em.persist(variant);
    return variant;
  }

  private Cart saveCart(Long userId) {
    Cart cart = Cart.builder().userId(userId).build();
    em.persist(cart);
    return cart;
  }

  private CartItem saveCartItem(Cart cart, ProductVariant variant, int quantity) {
    CartItem item = CartItem.builder()
        .cart(cart)
        .productVariant(variant)
        .quantity(quantity)
        .build();
    em.persist(item);
    return item;
  }

  private CartAddRequest addRequest(Long productId, Long variantId, Integer quantity) {
    CartAddRequest req = new CartAddRequest();
    ReflectionTestUtils.setField(req, "productId", productId);
    ReflectionTestUtils.setField(req, "variantId", variantId);
    ReflectionTestUtils.setField(req, "quantity", quantity);
    return req;
  }

  private CartQuantityUpdateRequest quantityUpdateRequest(Integer quantity) {
    CartQuantityUpdateRequest req = new CartQuantityUpdateRequest();
    ReflectionTestUtils.setField(req, "quantity", quantity);
    return req;
  }

  private CartOptionUpdateRequest optionUpdateRequest(Long variantId) {
    CartOptionUpdateRequest req = new CartOptionUpdateRequest();
    ReflectionTestUtils.setField(req, "variantId", variantId);
    return req;
  }
}
