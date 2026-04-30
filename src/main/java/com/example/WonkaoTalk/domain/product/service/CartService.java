package com.example.WonkaoTalk.domain.product.service;

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
import com.example.WonkaoTalk.domain.product.dto.CartResponse.CartItemInfo;
import com.example.WonkaoTalk.domain.product.dto.CartResponse.Summary;
import com.example.WonkaoTalk.domain.product.entity.Cart;
import com.example.WonkaoTalk.domain.product.entity.CartItem;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.CartItemRepository;
import com.example.WonkaoTalk.domain.product.repo.CartRepository;
import com.example.WonkaoTalk.domain.product.repo.ProductRepository;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepository;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final ProductRepository productRepository;
  private final ProductVariantRepository productVariantRepository;

  public CartResponse getCart(Long userId) {
    Optional<Cart> cartOpt = cartRepository.findByUserId(userId);

    if (cartOpt.isEmpty()) {
      return CartResponse.builder()
          .cartId(null)
          .cartItems(List.of())
          .summary(Summary.builder()
              .originalTotalAmount(0)
              .discountTotalAmount(0)
              .build())
          .build();
    }

    Cart cart = cartOpt.get();
    List<CartItem> cartItems = cartItemRepository.findAllWithVariantAndProductByCartId(
        cart.getId());

    List<CartItemInfo> cartItemInfos = cartItems.stream()
        .map(this::toCartItemInfo)
        .toList();

    int originalTotal = calculateOriginalTotal(cartItems);
    int discountTotal = calculateDiscountTotal(cartItems);

    return CartResponse.builder()
        .cartId(cart.getId())
        .cartItems(cartItemInfos)
        .summary(Summary.builder()
            .originalTotalAmount(originalTotal)
            .discountTotalAmount(discountTotal)
            .build())
        .build();
  }

  @Transactional
  public CartAddResponse addToCart(Long userId, CartAddRequest request) {
    if (request.getProductId() == null || request.getVariantId() == null
        || request.getQuantity() == null || request.getQuantity() <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }

    productRepository.findById(request.getProductId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    ProductVariant variant = productVariantRepository.findById(request.getVariantId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    if (!variant.getProduct().getId().equals(request.getProductId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    if (variant.getDeletedAt() != null || variant.getStatus() != SaleStatus.ON_SALE) {
      throw new BusinessException(ErrorCode.PROD_VARIANT_UNAVAILABLE);
    }

    Cart cart;
    Optional<Cart> cartOpt = cartRepository.findByUserIdWithLock(userId);
    if (cartOpt.isPresent()) {
      cart = cartOpt.get();
    } else {
      try {
        cart = cartRepository.save(Cart.builder().userId(userId).build());
      } catch (DataIntegrityViolationException e) {
        cart = cartRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SERVER_ERROR));
      }
    }

    Optional<CartItem> existingItem =
        cartItemRepository.findByCart_IdAndProductVariant_Id(cart.getId(), variant.getId());

    CartItem cartItem;
    if (existingItem.isPresent()) {
      cartItem = existingItem.get();
      int newQuantity = cartItem.getQuantity() + request.getQuantity();
      if (newQuantity > variant.getStock()) {
        throw new BusinessException(ErrorCode.PROD_STOCK_INSUFFICIENT);
      }
      cartItem.addQuantity(request.getQuantity());
    } else {
      if (request.getQuantity() > variant.getStock()) {
        throw new BusinessException(ErrorCode.PROD_STOCK_INSUFFICIENT);
      }
      cartItem = cartItemRepository.save(CartItem.builder()
          .cart(cart)
          .productVariant(variant)
          .quantity(request.getQuantity())
          .build());
    }

    return CartAddResponse.builder()
        .cartItemId(cartItem.getId())
        .build();
  }

  @Transactional
  public CartQuantityUpdateResponse updateCartItemQuantity(Long userId, Long cartItemId,
      CartQuantityUpdateRequest request) {
    if (request.getQuantity() == null || request.getQuantity() <= 0) {
      throw new BusinessException(ErrorCode.PROD_INVALID_QUANTITY);
    }

    CartItem cartItem = cartItemRepository.findWithVariantAndProductById(cartItemId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    Cart cart = cartRepository.findByUserId(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    if (!cartItem.getCart().getId().equals(cart.getId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    ProductVariant variant = cartItem.getProductVariant();
    if (variant.getStatus() != SaleStatus.ON_SALE || variant.getDeletedAt() != null) {
      throw new BusinessException(ErrorCode.PROD_VARIANT_UNAVAILABLE);
    }

    if (request.getQuantity() > variant.getStock()) {
      throw new BusinessException(ErrorCode.PROD_STOCK_INSUFFICIENT);
    }

    cartItem.updateQuantity(request.getQuantity());

    List<CartItem> allItems = cartItemRepository.findAllWithVariantAndProductByCartId(cart.getId());
    int originalTotal = calculateOriginalTotal(allItems);
    int discountTotal = calculateDiscountTotal(allItems);

    Product product = variant.getProduct();
    String updatedAt = cartItem.getUpdatedAt()
        .atZone(ZoneId.of("UTC"))
        .format(DateTimeFormatter.ISO_INSTANT);

    return CartQuantityUpdateResponse.builder()
        .originalTotalAmount(originalTotal)
        .discountTotalAmount(discountTotal)
        .cartItem(CartQuantityUpdateResponse.CartItemDetail.builder()
            .cartItemId(cartItem.getId())
            .id(product.getId())
            .name(product.getName())
            .variantId(variant.getId())
            .variantName(variant.getName())
            .price(product.getPrice())
            .discountedPrice(product.getDiscountedPrice())
            .quantity(cartItem.getQuantity())
            .stock(variant.getStock())
            .status(variant.getStatus().name())
            .updatedAt(updatedAt)
            .build())
        .build();
  }

  @Transactional
  public CartOptionUpdateResponse updateCartItemOption(Long userId, Long cartItemId,
      CartOptionUpdateRequest request) {
    if (request.getVariantId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }

    CartItem currentItem = cartItemRepository.findWithVariantAndProductById(cartItemId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    Cart cart = cartRepository.findByUserId(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    if (!currentItem.getCart().getId().equals(cart.getId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    ProductVariant targetVariant = productVariantRepository.findById(request.getVariantId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    if (targetVariant.getStatus() != SaleStatus.ON_SALE || targetVariant.getDeletedAt() != null) {
      throw new BusinessException(ErrorCode.PROD_VARIANT_UNAVAILABLE);
    }

    Optional<CartItem> duplicateOpt =
        cartItemRepository.findByCart_IdAndProductVariant_Id(cart.getId(), targetVariant.getId());

    CartItem resultItem;
    boolean isMerged;

    if (duplicateOpt.isPresent() && !duplicateOpt.get().getId().equals(currentItem.getId())) {
      CartItem duplicateItem = duplicateOpt.get();
      int mergedQuantity = duplicateItem.getQuantity() + currentItem.getQuantity();
      if (mergedQuantity > targetVariant.getStock()) {
        throw new BusinessException(ErrorCode.PROD_STOCK_INSUFFICIENT);
      }
      duplicateItem.updateQuantity(mergedQuantity);
      cartItemRepository.delete(currentItem);
      resultItem = duplicateItem;
      isMerged = true;
    } else {
      if (currentItem.getQuantity() > targetVariant.getStock()) {
        throw new BusinessException(ErrorCode.PROD_STOCK_INSUFFICIENT);
      }
      currentItem.updateVariant(targetVariant);
      resultItem = currentItem;
      isMerged = false;
    }

    List<CartItem> allItems = cartItemRepository.findAllWithVariantAndProductByCartId(cart.getId());
    int originalTotal = calculateOriginalTotal(allItems);
    int discountTotal = calculateDiscountTotal(allItems);

    Product product = targetVariant.getProduct();
    String updatedAt = resultItem.getUpdatedAt()
        .atZone(ZoneId.of("UTC"))
        .format(DateTimeFormatter.ISO_INSTANT);

    return CartOptionUpdateResponse.builder()
        .isMerged(isMerged)
        .originalTotalAmount(originalTotal)
        .discountTotalAmount(discountTotal)
        .cartItem(CartOptionUpdateResponse.CartItemDetail.builder()
            .cartItemId(resultItem.getId())
            .id(product.getId())
            .name(product.getName())
            .variantId(targetVariant.getId())
            .variantName(targetVariant.getName())
            .price(product.getPrice())
            .discountedPrice(product.getDiscountedPrice())
            .quantity(resultItem.getQuantity())
            .stock(targetVariant.getStock())
            .status(targetVariant.getStatus().name())
            .updatedAt(updatedAt)
            .build())
        .build();
  }

  @Transactional
  public CartDeleteResponse deleteFromCart(Long userId, List<Long> cartItemIds,
      boolean isAllDelete) {
    Cart cart = cartRepository.findByUserId(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    if (isAllDelete) {
      cartItemRepository.deleteByCart_Id(cart.getId());
      return CartDeleteResponse.builder().cartId(cart.getId()).build();
    }

    if (cartItemIds == null || cartItemIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }

    List<CartItem> items = cartItemRepository.findAllByIdInAndCart_Id(cartItemIds, cart.getId());
    if (items.size() != cartItemIds.size()) {
      // 요청한 ID 중 존재하지 않는 것 → 404, 타인 소유인 것 → 403으로 구분 불가하므로 소유권 우선 검증
      List<Long> foundIds = items.stream().map(CartItem::getId).toList();
      boolean hasUnowned = cartItemIds.stream().anyMatch(id -> !foundIds.contains(id));
      // findAllByIdInAndCart_Id가 이미 cart 소유권을 필터링하므로 미조회 = 미존재 또는 타인 소유
      // 타인 소유 여부를 판단하려면 cart 조건 없이 재조회
      boolean anyExistElsewhere = cartItemRepository.findAllById(
              cartItemIds.stream().filter(id -> !foundIds.contains(id)).toList())
          .stream().anyMatch(ci -> !ci.getCart().getId().equals(cart.getId()));

      if (anyExistElsewhere) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
      }
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    cartItemRepository.deleteAll(items);
    return CartDeleteResponse.builder().cartId(cart.getId()).build();
  }

  private int calculateOriginalTotal(List<CartItem> items) {
    return items.stream()
        .mapToInt(ci -> ci.getProductVariant().getProduct().getPrice() * ci.getQuantity())
        .sum();
  }

  private int calculateDiscountTotal(List<CartItem> items) {
    return items.stream()
        .mapToInt(ci -> ci.getProductVariant().getProduct().getDiscountedPrice() * ci.getQuantity())
        .sum();
  }

  private CartItemInfo toCartItemInfo(CartItem cartItem) {
    ProductVariant variant = cartItem.getProductVariant();
    Product product = variant.getProduct();

    String updatedAt = cartItem.getUpdatedAt()
        .atZone(ZoneId.of("UTC"))
        .format(DateTimeFormatter.ISO_INSTANT);

    return CartItemInfo.builder()
        .cartItemId(cartItem.getId())
        .id(product.getId())
        .name(product.getName())
        .price(product.getPrice())
        .discountedPrice(product.getDiscountedPrice())
        .discountRate(product.getDiscountRate())
        .thumbnail(product.getThumbnail())
        .variantId(variant.getId())
        .variantName(variant.getName())
        .quantity(cartItem.getQuantity())
        .stock(variant.getStock())
        .status(variant.getStatus().name())
        .updatedAt(updatedAt)
        .build();
  }
}
