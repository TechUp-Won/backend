package com.example.WonkaoTalk.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
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
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.CartItemRepository;
import com.example.WonkaoTalk.domain.product.repo.CartRepository;
import com.example.WonkaoTalk.domain.product.repo.ProductRepository;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
class CartServiceTest {

  @Mock private CartRepository cartRepository;
  @Mock private CartItemRepository cartItemRepository;
  @Mock private ProductRepository productRepository;
  @Mock private ProductVariantRepository productVariantRepository;

  @InjectMocks
  private CartService cartService;

  // ── getCart ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("장바구니가 없으면 cartId=null, cartItems=[], 합계=0을 반환한다")
  void getCart_returnsEmpty_whenCartNotFound() {
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

    CartResponse response = cartService.getCart(1L);

    assertThat(response.getCartId()).isNull();
    assertThat(response.getCartItems()).isEmpty();
    assertThat(response.getSummary().getOriginalTotalAmount()).isEqualTo(0);
    assertThat(response.getSummary().getDiscountTotalAmount()).isEqualTo(0);
  }

  @Test
  @DisplayName("originalTotalAmount는 price * quantity의 합산이다")
  void getCart_calculatesOriginalTotalAmount_correctly() {
    Cart cart = mockCart(1L, 1L);
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    CartItem item1 = mockCartItem(1L, cart, mockVariantWithProduct(1L, 10000, 8000), 2);
    CartItem item2 = mockCartItem(2L, cart, mockVariantWithProduct(2L, 5000, 4000), 3);
    when(cartItemRepository.findAllWithVariantAndProductByCartId(1L))
        .thenReturn(List.of(item1, item2));

    CartResponse response = cartService.getCart(1L);

    assertThat(response.getSummary().getOriginalTotalAmount()).isEqualTo(35000); // 10000*2 + 5000*3
  }

  @Test
  @DisplayName("discountTotalAmount는 discountedPrice * quantity의 합산이다")
  void getCart_calculatesDiscountTotalAmount_correctly() {
    Cart cart = mockCart(1L, 1L);
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    CartItem item1 = mockCartItem(1L, cart, mockVariantWithProduct(1L, 10000, 8000), 2);
    CartItem item2 = mockCartItem(2L, cart, mockVariantWithProduct(2L, 5000, 4000), 3);
    when(cartItemRepository.findAllWithVariantAndProductByCartId(1L))
        .thenReturn(List.of(item1, item2));

    CartResponse response = cartService.getCart(1L);

    assertThat(response.getSummary().getDiscountTotalAmount()).isEqualTo(28000); // 8000*2 + 4000*3
  }

  // ── addToCart - 입력 검증 ─────────────────────────────────────────────────────

  @Test
  @DisplayName("productId가 null이면 BAD_REQUEST를 던진다")
  void addToCart_throwsBadRequest_whenProductIdIsNull() {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.addToCart(1L, mockAddRequest(null, 1L, 2)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  @DisplayName("variantId가 null이면 BAD_REQUEST를 던진다")
  void addToCart_throwsBadRequest_whenVariantIdIsNull() {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.addToCart(1L, mockAddRequest(1L, null, 2)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  @DisplayName("quantity가 null이면 BAD_REQUEST를 던진다")
  void addToCart_throwsBadRequest_whenQuantityIsNull() {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.addToCart(1L, mockAddRequest(1L, 1L, null)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  @DisplayName("quantity가 0이면 BAD_REQUEST를 던진다")
  void addToCart_throwsBadRequest_whenQuantityIsZero() {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.addToCart(1L, mockAddRequest(1L, 1L, 0)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  // ── addToCart - 상품/옵션 검증 ───────────────────────────────────────────────

  @Test
  @DisplayName("productId에 해당하는 상품이 없으면 NOT_FOUND를 던진다")
  void addToCart_throwsNotFound_whenProductNotFound() {
    when(productRepository.findById(99L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.addToCart(1L, mockAddRequest(99L, 1L, 2)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  @DisplayName("variantId에 해당하는 variant가 없으면 NOT_FOUND를 던진다")
  void addToCart_throwsNotFound_whenVariantNotFound() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(mock(Product.class)));
    when(productVariantRepository.findById(99L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.addToCart(1L, mockAddRequest(1L, 99L, 2)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  @DisplayName("variant가 해당 product 소속이 아니면 NOT_FOUND를 던진다")
  void addToCart_throwsNotFound_whenVariantNotBelongsToProduct() {
    Product product = mockProductWithId(1L);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    Product otherProduct = mockProductWithId(99L);
    ProductVariant variant = mockVariant(1L, otherProduct, 10, SaleStatus.ON_SALE, null);
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.addToCart(1L, mockAddRequest(1L, 1L, 2)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  @DisplayName("variant가 삭제된 상태이면 PROD_VARIANT_UNAVAILABLE을 던진다")
  void addToCart_throwsUnavailable_whenVariantIsDeleted() {
    Product product = mockProductWithId(1L);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    ProductVariant variant = mockVariant(1L, product, 10, SaleStatus.ON_SALE, LocalDateTime.now());
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.addToCart(1L, mockAddRequest(1L, 1L, 2)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_VARIANT_UNAVAILABLE);
  }

  @Test
  @DisplayName("variant의 status가 ON_SALE이 아니면 PROD_VARIANT_UNAVAILABLE을 던진다")
  void addToCart_throwsUnavailable_whenVariantNotOnSale() {
    Product product = mockProductWithId(1L);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    ProductVariant variant = mockVariant(1L, product, 10, SaleStatus.STOP_SALE, null);
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.addToCart(1L, mockAddRequest(1L, 1L, 2)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_VARIANT_UNAVAILABLE);
  }

  @Test
  @DisplayName("신규 담기 시 요청 수량이 재고를 초과하면 PROD_STOCK_INSUFFICIENT를 던진다")
  void addToCart_throwsStockInsufficient_whenQuantityExceedsStock() {
    Product product = mockProductWithId(1L);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    ProductVariant variant = mockVariant(1L, product, 3, SaleStatus.ON_SALE, null);
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));

    Cart cart = mockCart(1L, 1L);
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findByCart_IdAndProductVariant_Id(1L, 1L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.addToCart(1L, mockAddRequest(1L, 1L, 5))); // 5 > stock 3

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_STOCK_INSUFFICIENT);
  }

  // ── addToCart - Upsert ───────────────────────────────────────────────────────

  @Test
  @DisplayName("장바구니가 없으면 자동으로 새로 생성된다")
  void addToCart_createsNewCart_whenCartNotFound() {
    Product product = mockProductWithId(1L);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    ProductVariant variant = mockVariant(1L, product, 10, SaleStatus.ON_SALE, null);
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));

    when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
    Cart newCart = mockCart(1L, 1L);
    when(cartRepository.save(any(Cart.class))).thenReturn(newCart);
    when(cartItemRepository.findByCart_IdAndProductVariant_Id(1L, 1L)).thenReturn(Optional.empty());

    CartItem savedItem = mockCartItem(10L, newCart, variant, 2);
    when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedItem);

    CartAddResponse response = cartService.addToCart(1L, mockAddRequest(1L, 1L, 2));

    verify(cartRepository).save(any(Cart.class));
    assertThat(response.getCartItemId()).isEqualTo(10L);
  }

  @Test
  @DisplayName("동일한 variant가 이미 장바구니에 있으면 addQuantity가 호출된다")
  void addToCart_callsAddQuantity_whenSameVariantExists() {
    Product product = mockProductWithId(1L);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    ProductVariant variant = mockVariant(1L, product, 10, SaleStatus.ON_SALE, null);
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));

    Cart cart = mockCart(1L, 1L);
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    CartItem existingItem = mockCartItem(5L, cart, variant, 3);
    when(cartItemRepository.findByCart_IdAndProductVariant_Id(1L, 1L))
        .thenReturn(Optional.of(existingItem));

    CartAddResponse response = cartService.addToCart(1L, mockAddRequest(1L, 1L, 2));

    verify(existingItem).addQuantity(2);
    verify(cartItemRepository, never()).save(any(CartItem.class));
    assertThat(response.getCartItemId()).isEqualTo(5L);
  }

  @Test
  @DisplayName("Upsert 시 합산 수량이 재고를 초과하면 PROD_STOCK_INSUFFICIENT를 던진다")
  void addToCart_throwsStockInsufficient_whenMergedQuantityExceedsStock() {
    Product product = mockProductWithId(1L);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    ProductVariant variant = mockVariant(1L, product, 5, SaleStatus.ON_SALE, null);
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));

    Cart cart = mockCart(1L, 1L);
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    CartItem existingItem = mockCartItem(5L, cart, variant, 4); // 기존 4 + 요청 3 = 7 > stock 5
    when(cartItemRepository.findByCart_IdAndProductVariant_Id(1L, 1L))
        .thenReturn(Optional.of(existingItem));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.addToCart(1L, mockAddRequest(1L, 1L, 3)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_STOCK_INSUFFICIENT);
  }

  // ── updateCartItemQuantity - 입력 검증 ──────────────────────────────────────

  @Test
  @DisplayName("quantity가 null이면 PROD_INVALID_QUANTITY를 던진다")
  void updateQuantity_throwsInvalidQuantity_whenQuantityIsNull() {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemQuantity(1L, 1L, mockQuantityUpdateRequest(null)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_QUANTITY);
  }

  @Test
  @DisplayName("quantity가 0이면 PROD_INVALID_QUANTITY를 던진다")
  void updateQuantity_throwsInvalidQuantity_whenQuantityIsZero() {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemQuantity(1L, 1L, mockQuantityUpdateRequest(0)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_INVALID_QUANTITY);
  }

  // ── updateCartItemQuantity - 소유권/상태 검증 ────────────────────────────────

  @Test
  @DisplayName("cartItemId에 해당하는 아이템이 없으면 NOT_FOUND를 던진다")
  void updateQuantity_throwsNotFound_whenCartItemNotFound() {
    when(cartItemRepository.findWithVariantAndProductById(99L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemQuantity(1L, 99L, mockQuantityUpdateRequest(3)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  @DisplayName("사용자의 장바구니가 없으면 NOT_FOUND를 던진다")
  void updateQuantity_throwsNotFound_whenCartNotFound() {
    Cart cart = mockCart(1L, 1L);
    CartItem cartItem = mockCartItem(1L, cart, mockVariantWithProduct(1L, 10000, 8000), 2);
    when(cartItemRepository.findWithVariantAndProductById(1L)).thenReturn(Optional.of(cartItem));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemQuantity(1L, 1L, mockQuantityUpdateRequest(3)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  @DisplayName("아이템이 다른 사용자의 장바구니에 속하면 FORBIDDEN을 던진다")
  void updateQuantity_throwsForbidden_whenCartItemNotOwnedByUser() {
    Cart userCart = mockCart(1L, 1L);
    Cart otherCart = mockCart(2L, 99L);
    CartItem cartItem = mockCartItem(1L, otherCart, mockVariantWithProduct(1L, 10000, 8000), 2);
    when(cartItemRepository.findWithVariantAndProductById(1L)).thenReturn(Optional.of(cartItem));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(userCart));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemQuantity(1L, 1L, mockQuantityUpdateRequest(3)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("variant의 status가 ON_SALE이 아니면 PROD_VARIANT_UNAVAILABLE을 던진다")
  void updateQuantity_throwsUnavailable_whenVariantNotOnSale() {
    Cart cart = mockCart(1L, 1L);
    ProductVariant variant = mockVariantWithProduct(1L, 10000, 8000);
    when(variant.getStatus()).thenReturn(SaleStatus.STOP_SALE);
    CartItem cartItem = mockCartItem(1L, cart, variant, 2);
    when(cartItemRepository.findWithVariantAndProductById(1L)).thenReturn(Optional.of(cartItem));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemQuantity(1L, 1L, mockQuantityUpdateRequest(3)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_VARIANT_UNAVAILABLE);
  }

  @Test
  @DisplayName("요청 수량이 재고를 초과하면 PROD_STOCK_INSUFFICIENT를 던진다")
  void updateQuantity_throwsStockInsufficient_whenQuantityExceedsStock() {
    Cart cart = mockCart(1L, 1L);
    ProductVariant variant = mockVariantWithProduct(1L, 10000, 8000);
    when(variant.getStock()).thenReturn(3);
    CartItem cartItem = mockCartItem(1L, cart, variant, 2);
    when(cartItemRepository.findWithVariantAndProductById(1L)).thenReturn(Optional.of(cartItem));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemQuantity(1L, 1L, mockQuantityUpdateRequest(5))); // 5 > stock 3

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_STOCK_INSUFFICIENT);
  }

  // ── updateCartItemQuantity - 성공 ────────────────────────────────────────────

  @Test
  @DisplayName("수량 수정 성공 시 updateQuantity가 요청값으로 호출된다")
  void updateQuantity_callsUpdateQuantity_withRequestedValue() {
    Cart cart = mockCart(1L, 1L);
    ProductVariant variant = mockVariantWithProduct(1L, 10000, 8000);
    CartItem cartItem = mockCartItem(1L, cart, variant, 2);
    when(cartItemRepository.findWithVariantAndProductById(1L)).thenReturn(Optional.of(cartItem));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findAllWithVariantAndProductByCartId(1L)).thenReturn(List.of(cartItem));

    cartService.updateCartItemQuantity(1L, 1L, mockQuantityUpdateRequest(5));

    verify(cartItem).updateQuantity(5);
  }

  @Test
  @DisplayName("수량 수정 후 전체 합계 금액이 재계산되어 반환된다")
  void updateQuantity_recalculatesTotalAmounts_afterUpdate() {
    Cart cart = mockCart(1L, 1L);
    ProductVariant variant = mockVariantWithProduct(1L, 10000, 8000);
    CartItem cartItem = mockCartItem(1L, cart, variant, 3);
    when(cartItemRepository.findWithVariantAndProductById(1L)).thenReturn(Optional.of(cartItem));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findAllWithVariantAndProductByCartId(1L)).thenReturn(List.of(cartItem));

    CartQuantityUpdateResponse response =
        cartService.updateCartItemQuantity(1L, 1L, mockQuantityUpdateRequest(3));

    assertThat(response.getOriginalTotalAmount()).isEqualTo(30000); // 10000 * 3
    assertThat(response.getDiscountTotalAmount()).isEqualTo(24000); // 8000 * 3
  }

  // ── updateCartItemOption - 검증 ──────────────────────────────────────────────

  @Test
  @DisplayName("variantId가 null이면 BAD_REQUEST를 던진다")
  void updateOption_throwsBadRequest_whenVariantIdIsNull() {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemOption(1L, 1L, mockOptionUpdateRequest(null)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  @DisplayName("cartItemId에 해당하는 아이템이 없으면 NOT_FOUND를 던진다")
  void updateOption_throwsNotFound_whenCartItemNotFound() {
    when(cartItemRepository.findWithVariantAndProductById(99L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemOption(1L, 99L, mockOptionUpdateRequest(2L)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  @DisplayName("아이템이 다른 사용자의 장바구니에 속하면 FORBIDDEN을 던진다")
  void updateOption_throwsForbidden_whenNotOwnedByUser() {
    Cart userCart = mockCart(1L, 1L);
    Cart otherCart = mockCart(2L, 99L);
    CartItem cartItem = mockCartItem(1L, otherCart, mockVariantWithProduct(1L, 10000, 8000), 2);
    when(cartItemRepository.findWithVariantAndProductById(1L)).thenReturn(Optional.of(cartItem));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(userCart));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemOption(1L, 1L, mockOptionUpdateRequest(2L)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("targetVariant가 존재하지 않으면 NOT_FOUND를 던진다")
  void updateOption_throwsNotFound_whenTargetVariantNotFound() {
    Cart cart = mockCart(1L, 1L);
    CartItem cartItem = mockCartItem(1L, cart, mockVariantWithProduct(1L, 10000, 8000), 2);
    when(cartItemRepository.findWithVariantAndProductById(1L)).thenReturn(Optional.of(cartItem));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
    when(productVariantRepository.findById(99L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemOption(1L, 1L, mockOptionUpdateRequest(99L)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  @DisplayName("targetVariant의 status가 ON_SALE이 아니면 PROD_VARIANT_UNAVAILABLE을 던진다")
  void updateOption_throwsUnavailable_whenTargetVariantNotOnSale() {
    Cart cart = mockCart(1L, 1L);
    CartItem cartItem = mockCartItem(1L, cart, mockVariantWithProduct(1L, 10000, 8000), 2);
    when(cartItemRepository.findWithVariantAndProductById(1L)).thenReturn(Optional.of(cartItem));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    ProductVariant targetVariant = mockVariant(2L, mockProductWithId(2L), 10, SaleStatus.STOP_SALE, null);
    when(productVariantRepository.findById(2L)).thenReturn(Optional.of(targetVariant));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemOption(1L, 1L, mockOptionUpdateRequest(2L)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_VARIANT_UNAVAILABLE);
  }

  // ── updateCartItemOption - Merge ─────────────────────────────────────────────

  @Test
  @DisplayName("Merge 시 합산 수량이 재고를 초과하면 PROD_STOCK_INSUFFICIENT를 던진다")
  void updateOption_throwsStockInsufficient_whenMergedQuantityExceedsStock() {
    Cart cart = mockCart(1L, 1L);
    CartItem currentItem = mockCartItem(1L, cart, mockVariantWithProduct(1L, 10000, 8000), 3);
    when(cartItemRepository.findWithVariantAndProductById(1L)).thenReturn(Optional.of(currentItem));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    ProductVariant targetVariant = mockVariant(2L, mockProductWithId(2L), 4, SaleStatus.ON_SALE, null);
    when(productVariantRepository.findById(2L)).thenReturn(Optional.of(targetVariant));

    CartItem duplicateItem = mockCartItem(2L, cart, targetVariant, 3); // 기존 3 + 현재 3 = 6 > stock 4
    when(cartItemRepository.findByCart_IdAndProductVariant_Id(1L, 2L))
        .thenReturn(Optional.of(duplicateItem));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemOption(1L, 1L, mockOptionUpdateRequest(2L)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_STOCK_INSUFFICIENT);
  }

  @Test
  @DisplayName("Merge 발생 시 isMerged=true이고 currentItem이 삭제되며 수량이 합산된다")
  void updateOption_merges_andDeletesCurrentItem_whenDuplicateExists() {
    Cart cart = mockCart(1L, 1L);
    CartItem currentItem = mockCartItem(1L, cart, mockVariantWithProduct(1L, 10000, 8000), 2);
    when(cartItemRepository.findWithVariantAndProductById(1L)).thenReturn(Optional.of(currentItem));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    ProductVariant targetVariant = mockVariant(2L, mockProductWithId(2L), 10, SaleStatus.ON_SALE, null);
    when(productVariantRepository.findById(2L)).thenReturn(Optional.of(targetVariant));

    CartItem duplicateItem = mockCartItem(2L, cart, targetVariant, 3);
    when(cartItemRepository.findByCart_IdAndProductVariant_Id(1L, 2L))
        .thenReturn(Optional.of(duplicateItem));
    when(cartItemRepository.findAllWithVariantAndProductByCartId(1L))
        .thenReturn(List.of(duplicateItem));

    CartOptionUpdateResponse response =
        cartService.updateCartItemOption(1L, 1L, mockOptionUpdateRequest(2L));

    assertThat(response.isMerged()).isTrue();
    verify(duplicateItem).updateQuantity(5); // 3 + 2 = 5
    verify(cartItemRepository).delete(currentItem);
    assertThat(response.getCartItem().getCartItemId()).isEqualTo(2L);
  }

  // ── updateCartItemOption - Update ────────────────────────────────────────────

  @Test
  @DisplayName("현재 아이템과 동일한 variant를 선택하면 삭제 없이 isMerged=false로 반환된다")
  void updateOption_noChange_whenSameVariantSelected() {
    Cart cart = mockCart(1L, 1L);
    ProductVariant sameVariant = mockVariant(1L, mockProductWithId(1L), 10, SaleStatus.ON_SALE, null);
    CartItem currentItem = mockCartItem(1L, cart, sameVariant, 2);
    when(cartItemRepository.findWithVariantAndProductById(1L)).thenReturn(Optional.of(currentItem));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(sameVariant));
    when(cartItemRepository.findByCart_IdAndProductVariant_Id(1L, 1L))
        .thenReturn(Optional.of(currentItem)); // 자기 자신을 반환
    when(cartItemRepository.findAllWithVariantAndProductByCartId(1L)).thenReturn(List.of(currentItem));

    CartOptionUpdateResponse response =
        cartService.updateCartItemOption(1L, 1L, mockOptionUpdateRequest(1L));

    assertThat(response.isMerged()).isFalse();
    verify(cartItemRepository, never()).delete(any());
    assertThat(response.getCartItem().getCartItemId()).isEqualTo(1L);
    assertThat(response.getCartItem().getQuantity()).isEqualTo(2);
  }

  @Test
  @DisplayName("동일한 variant가 없으면 isMerged=false이고 updateVariant가 호출된다")
  void updateOption_updatesVariant_whenNoDuplicateExists() {
    Cart cart = mockCart(1L, 1L);
    CartItem currentItem = mockCartItem(1L, cart, mockVariantWithProduct(1L, 10000, 8000), 2);
    when(cartItemRepository.findWithVariantAndProductById(1L)).thenReturn(Optional.of(currentItem));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    ProductVariant targetVariant = mockVariant(2L, mockProductWithId(2L), 10, SaleStatus.ON_SALE, null);
    when(productVariantRepository.findById(2L)).thenReturn(Optional.of(targetVariant));
    when(cartItemRepository.findByCart_IdAndProductVariant_Id(1L, 2L)).thenReturn(Optional.empty());
    when(cartItemRepository.findAllWithVariantAndProductByCartId(1L)).thenReturn(List.of(currentItem));

    CartOptionUpdateResponse response =
        cartService.updateCartItemOption(1L, 1L, mockOptionUpdateRequest(2L));

    assertThat(response.isMerged()).isFalse();
    verify(currentItem).updateVariant(targetVariant);
    assertThat(response.getCartItem().getCartItemId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Update 시 기존 수량이 재고를 초과하면 PROD_STOCK_INSUFFICIENT를 던진다")
  void updateOption_throwsStockInsufficient_whenCurrentQuantityExceedsStock() {
    Cart cart = mockCart(1L, 1L);
    CartItem currentItem = mockCartItem(1L, cart, mockVariantWithProduct(1L, 10000, 8000), 5);
    when(cartItemRepository.findWithVariantAndProductById(1L)).thenReturn(Optional.of(currentItem));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    ProductVariant targetVariant = mockVariant(2L, mockProductWithId(2L), 3, SaleStatus.ON_SALE, null); // stock=3
    when(productVariantRepository.findById(2L)).thenReturn(Optional.of(targetVariant));
    when(cartItemRepository.findByCart_IdAndProductVariant_Id(1L, 2L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.updateCartItemOption(1L, 1L, mockOptionUpdateRequest(2L))); // quantity 5 > stock 3

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_STOCK_INSUFFICIENT);
  }

  // ── deleteFromCart ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("장바구니가 없으면 NOT_FOUND를 던진다")
  void deleteFromCart_throwsNotFound_whenCartNotFound() {
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.deleteFromCart(1L, null, true));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  @DisplayName("isAllDelete=true이면 deleteByCart_Id가 호출되고 cartId를 반환한다")
  void deleteFromCart_deletesAll_whenIsAllDeleteIsTrue() {
    Cart cart = mockCart(1L, 1L);
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    CartDeleteResponse response = cartService.deleteFromCart(1L, null, true);

    verify(cartItemRepository).deleteByCart_Id(1L);
    assertThat(response.getCartId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("isAllDelete=false이고 cartItemIds가 null이면 BAD_REQUEST를 던진다")
  void deleteFromCart_throwsBadRequest_whenCartItemIdsIsNull() {
    Cart cart = mockCart(1L, 1L);
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.deleteFromCart(1L, null, false));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  @DisplayName("isAllDelete=false이고 cartItemIds가 비어 있으면 BAD_REQUEST를 던진다")
  void deleteFromCart_throwsBadRequest_whenCartItemIdsIsEmpty() {
    Cart cart = mockCart(1L, 1L);
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.deleteFromCart(1L, List.of(), false));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  @DisplayName("요청한 아이템이 타인의 장바구니에 속하면 FORBIDDEN을 던진다")
  void deleteFromCart_throwsForbidden_whenItemBelongsToOtherCart() {
    Cart userCart = mockCart(1L, 1L);
    Cart otherCart = mockCart(2L, 99L);
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(userCart));
    when(cartItemRepository.findAllByIdInAndCart_Id(List.of(10L), 1L)).thenReturn(List.of());

    CartItem otherItem = mock(CartItem.class);
    when(otherItem.getCart()).thenReturn(otherCart);
    when(cartItemRepository.findAllById(List.of(10L))).thenReturn(List.of(otherItem));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.deleteFromCart(1L, List.of(10L), false));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("존재하지 않는 아이템 ID이면 NOT_FOUND를 던진다")
  void deleteFromCart_throwsNotFound_whenItemNotFound() {
    Cart cart = mockCart(1L, 1L);
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findAllByIdInAndCart_Id(List.of(999L), 1L)).thenReturn(List.of());
    when(cartItemRepository.findAllById(List.of(999L))).thenReturn(List.of());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> cartService.deleteFromCart(1L, List.of(999L), false));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  @DisplayName("선택 삭제 성공 시 해당 아이템들에 대해 deleteAll이 호출된다")
  void deleteFromCart_deletesSelectedItems_successfully() {
    Cart cart = mockCart(1L, 1L);
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    CartItem item1 = mockCartItem(1L, cart, mockVariantWithProduct(1L, 10000, 8000), 2);
    CartItem item2 = mockCartItem(2L, cart, mockVariantWithProduct(2L, 5000, 4000), 1);
    when(cartItemRepository.findAllByIdInAndCart_Id(List.of(1L, 2L), 1L))
        .thenReturn(List.of(item1, item2));

    CartDeleteResponse response = cartService.deleteFromCart(1L, List.of(1L, 2L), false);

    verify(cartItemRepository).deleteAll(List.of(item1, item2));
    assertThat(response.getCartId()).isEqualTo(1L);
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

  private Cart mockCart(Long cartId, Long userId) {
    Cart cart = mock(Cart.class);
    when(cart.getId()).thenReturn(cartId);
    when(cart.getUserId()).thenReturn(userId);
    return cart;
  }

  private CartItem mockCartItem(Long id, Cart cart, ProductVariant variant, Integer quantity) {
    CartItem item = mock(CartItem.class);
    when(item.getId()).thenReturn(id);
    when(item.getCart()).thenReturn(cart);
    when(item.getProductVariant()).thenReturn(variant);
    when(item.getQuantity()).thenReturn(quantity);
    when(item.getUpdatedAt()).thenReturn(LocalDateTime.now());
    return item;
  }

  private ProductVariant mockVariant(Long id, Product product, int stock, SaleStatus status,
      LocalDateTime deletedAt) {
    ProductVariant variant = mock(ProductVariant.class);
    when(variant.getId()).thenReturn(id);
    when(variant.getProduct()).thenReturn(product);
    when(variant.getStock()).thenReturn(stock);
    when(variant.getStatus()).thenReturn(status);
    when(variant.getDeletedAt()).thenReturn(deletedAt);
    when(variant.getName()).thenReturn("옵션" + id);
    return variant;
  }

  private ProductVariant mockVariantWithProduct(Long id, int price, int discountedPrice) {
    Product product = mock(Product.class);
    when(product.getId()).thenReturn(id);
    when(product.getName()).thenReturn("상품" + id);
    when(product.getPrice()).thenReturn(price);
    when(product.getDiscountedPrice()).thenReturn(discountedPrice);

    ProductVariant variant = mock(ProductVariant.class);
    when(variant.getId()).thenReturn(id);
    when(variant.getProduct()).thenReturn(product);
    when(variant.getStock()).thenReturn(50);
    when(variant.getStatus()).thenReturn(SaleStatus.ON_SALE);
    when(variant.getDeletedAt()).thenReturn(null);
    when(variant.getName()).thenReturn("옵션" + id);
    return variant;
  }

  private Product mockProductWithId(Long id) {
    Product product = mock(Product.class);
    when(product.getId()).thenReturn(id);
    when(product.getName()).thenReturn("상품" + id);
    when(product.getPrice()).thenReturn(10000);
    when(product.getDiscountedPrice()).thenReturn(8000);
    return product;
  }

  private CartAddRequest mockAddRequest(Long productId, Long variantId, Integer quantity) {
    CartAddRequest request = mock(CartAddRequest.class);
    when(request.getProductId()).thenReturn(productId);
    when(request.getVariantId()).thenReturn(variantId);
    when(request.getQuantity()).thenReturn(quantity);
    return request;
  }

  private CartQuantityUpdateRequest mockQuantityUpdateRequest(Integer quantity) {
    CartQuantityUpdateRequest request = mock(CartQuantityUpdateRequest.class);
    when(request.getQuantity()).thenReturn(quantity);
    return request;
  }

  private CartOptionUpdateRequest mockOptionUpdateRequest(Long variantId) {
    CartOptionUpdateRequest request = mock(CartOptionUpdateRequest.class);
    when(request.getVariantId()).thenReturn(variantId);
    return request;
  }
}
