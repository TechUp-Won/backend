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
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
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
  private final UserRepo userRepo;

  public CartResponse getCart(Long authId) {
    Long userId = resolveUser(authId).getId();
    Optional<Cart> cartOpt = cartRepository.findByUser_Id(userId);

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
  public CartAddResponse addToCart(Long authId, CartAddRequest request) {
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

    User user = resolveUser(authId);
    Long userId = user.getId();

    Cart cart;
    Optional<Cart> cartOpt = cartRepository.findByUserIdWithLock(userId);
    if (cartOpt.isPresent()) {
      cart = cartOpt.get();
    } else {
      try {
        cart = cartRepository.save(Cart.builder().user(user).build());
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
  public CartQuantityUpdateResponse updateCartItemQuantity(Long authId, Long cartItemId,
      CartQuantityUpdateRequest request) {
    if (request.getQuantity() == null || request.getQuantity() <= 0) {
      throw new BusinessException(ErrorCode.PROD_INVALID_QUANTITY);
    }

    Long userId = resolveUser(authId).getId();

    CartItem cartItem = cartItemRepository.findWithVariantAndProductByIdAndUserId(cartItemId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    ProductVariant variant = cartItem.getProductVariant();
    if (variant.getStatus() != SaleStatus.ON_SALE || variant.getDeletedAt() != null) {
      throw new BusinessException(ErrorCode.PROD_VARIANT_UNAVAILABLE);
    }

    if (request.getQuantity() > variant.getStock()) {
      throw new BusinessException(ErrorCode.PROD_STOCK_INSUFFICIENT);
    }

    cartItem.updateQuantity(request.getQuantity());

    List<CartItem> allItems = cartItemRepository.findAllWithVariantAndProductByCartId(cartItem.getCart().getId());
    int originalTotal = calculateOriginalTotal(allItems);
    int discountTotal = calculateDiscountTotal(allItems);

    Product product = variant.getProduct();

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
            .updatedAt(cartItem.getUpdatedAt())
            .build())
        .build();
  }

  @Transactional
  public CartOptionUpdateResponse updateCartItemOption(Long authId, Long cartItemId,
      CartOptionUpdateRequest request) {
    if (request.getVariantId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }

    Long userId = resolveUser(authId).getId();

    CartItem currentItem = cartItemRepository.findWithVariantAndProductByIdAndUserId(cartItemId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    Long cartId = currentItem.getCart().getId();

    ProductVariant targetVariant = productVariantRepository.findById(request.getVariantId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    if (targetVariant.getStatus() != SaleStatus.ON_SALE || targetVariant.getDeletedAt() != null) {
      throw new BusinessException(ErrorCode.PROD_VARIANT_UNAVAILABLE);
    }

    Optional<CartItem> duplicateOpt =
        cartItemRepository.findByCart_IdAndProductVariant_Id(cartId, targetVariant.getId());

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

    List<CartItem> allItems = cartItemRepository.findAllWithVariantAndProductByCartId(cartId);
    int originalTotal = calculateOriginalTotal(allItems);
    int discountTotal = calculateDiscountTotal(allItems);

    Product product = targetVariant.getProduct();

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
            .updatedAt(resultItem.getUpdatedAt())
            .build())
        .build();
  }

  @Transactional
  public CartDeleteResponse deleteFromCart(Long authId, List<Long> cartItemIds,
      boolean isAllDelete) {
    Long userId = resolveUser(authId).getId();
    Cart cart = cartRepository.findByUser_Id(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    if (isAllDelete) {
      cartItemRepository.deleteByCart_Id(cart.getId());
      return CartDeleteResponse.builder().cartId(cart.getId()).build();
    }

    if (cartItemIds == null || cartItemIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }

    int count = cartItemRepository.countByIdInAndCart_Id(cartItemIds, cart.getId());
    if (count != cartItemIds.size()) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    cartItemRepository.deleteAllByIdInAndCart_Id(cartItemIds, cart.getId());
    return CartDeleteResponse.builder().cartId(cart.getId()).build();
  }

  private User resolveUser(Long authId) {
    return userRepo.findByAuth_Id(authId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
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
        .updatedAt(cartItem.getUpdatedAt())
        .build();
  }
}
