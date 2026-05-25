package com.example.WonkaoTalk.domain.product.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.image.service.ImageService;
import com.example.WonkaoTalk.domain.product.dto.ProductEditFormResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductEditFormResponse.ImageInfo;
import com.example.WonkaoTalk.domain.product.dto.ProductEditFormResponse.OptionGroupInfo;
import com.example.WonkaoTalk.domain.product.dto.ProductEditFormResponse.OptionInfo;
import com.example.WonkaoTalk.domain.product.dto.ProductEditFormResponse.VariantInfo;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductUpdateService {

  private final SellerRepo sellerRepo;
  private final StoreRepo storeRepo;
  private final ProductRepo productRepo;
  private final ProductDetailRepo productDetailRepo;
  private final ProductImageRepo productImageRepo;
  private final DeletedProductImageRepo deletedProductImageRepo;
  private final ProductOptionGroupRepo productOptionGroupRepo;
  private final ProductOptionRepo productOptionRepo;
  private final ProductVariantRepo productVariantRepo;
  private final CategoryRepo categoryRepo;
  private final ImageService imageService;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional(readOnly = true)
  public ProductEditFormResponse getEditForm(Long authId, Long productId) {
    Store store = resolveStore(authId);
    Product product = findOwnProduct(productId, store);

    List<ImageInfo> images = productImageRepo.findByProductIdOrderBySortOrderAsc(productId)
        .stream()
        .map(img -> ImageInfo.builder()
            .imageId(img.getId())
            .url(img.getUrl())
            .sortOrder(img.getSortOrder())
            .build())
        .toList();

    String detail = productDetailRepo.findFirstByProductId(productId)
        .map(ProductDetail::getContent)
        .orElse(null);

    List<ProductOptionGroup> groups = productOptionGroupRepo.findByProductId(productId);
    List<Long> groupIds = groups.stream().map(ProductOptionGroup::getId).toList();
    Map<Long, List<ProductOption>> optionsByGroup = productOptionRepo
        .findByProductOptionGroupIdIn(groupIds)
        .stream()
        .collect(Collectors.groupingBy(opt -> opt.getProductOptionGroup().getId()));

    List<OptionGroupInfo> optionGroups = groups.stream()
        .map(group -> {
          List<OptionInfo> options = optionsByGroup.getOrDefault(group.getId(), List.of())
              .stream()
              .map(opt -> OptionInfo.builder()
                  .optionId(opt.getId())
                  .name(opt.getName())
                  .build())
              .toList();
          return OptionGroupInfo.builder()
              .optionGroupId(group.getId())
              .name(group.getName())
              .sortOrder(group.getSortOrder())
              .options(options)
              .build();
        })
        .toList();

    List<VariantInfo> variants = productVariantRepo.findByProductId(productId).stream()
        .map(v -> VariantInfo.builder()
            .variantId(v.getId())
            .variantName(v.getName())
            .stock(v.getStock())
            .status(v.getStatus().name())
            .build())
        .toList();

    return ProductEditFormResponse.builder()
        .productId(product.getId())
        .name(product.getName())
        .categoryId(product.getCategory().getId())
        .thumbnail(product.getThumbnail())
        .price(product.getPrice())
        .discountRate(product.getDiscountRate())
        .discountedPrice(product.getDiscountedPrice())
        .status(product.getStatus().name())
        .detail(detail)
        .images(images)
        .optionGroups(optionGroups)
        .variants(variants)
        .build();
  }

  @Transactional
  public ProductUpdateResponse update(Long authId, Long productId, ProductUpdateRequest request) {
    Store store = resolveStore(authId);
    Product product = findOwnProduct(productId, store);

    validateRequest(request);

    Category category = null;
    if (request.categoryId() != null) {
      category = categoryRepo.findById(request.categoryId())
          .orElseThrow(() -> new BusinessException(ErrorCode.PROD_CATEGORY_NOT_FOUND));
    }

    List<String> objectKeysToMove = new ArrayList<>();

    String thumbnailUrl = null;
    if (request.thumbnailKey() != null) {
      if (product.getThumbnail() != null) {
        deletedProductImageRepo.save(
            DeletedProductImage.builder().url(product.getThumbnail()).build());
      }
      thumbnailUrl = imageService.validateAndGetProductUrl(request.thumbnailKey());
      objectKeysToMove.add(request.thumbnailKey());
    }

    if (request.images() != null) {
      processImages(product, request.images(), objectKeysToMove);
    }

    if (request.detail() != null) {
      processDetail(product, request.detail());
    }

    SaleStatus newStatus = request.status() != null ? SaleStatus.valueOf(request.status()) : null;
    product.update(request.name(), category, thumbnailUrl, request.price(), request.discountRate(),
        newStatus);

    productRepo.save(product);

    if (!objectKeysToMove.isEmpty()) {
      eventPublisher.publishEvent(new ProductCreatedEvent(objectKeysToMove));
    }

    return new ProductUpdateResponse(
        product.getId(),
        product.getName(),
        product.getCategory().getId(),
        product.getThumbnail(),
        product.getPrice(),
        product.getDiscountRate(),
        product.getDiscountedPrice(),
        product.getStatus().name(),
        product.getUpdatedAt()
    );
  }

  private void processImages(Product product, List<ImageRequest> images,
      List<String> objectKeysToMove) {
    List<ProductImage> currentImages = productImageRepo.findByProductIdOrderBySortOrderAsc(
        product.getId());

    if (images.isEmpty()) {
      recordDeletedUrls(currentImages);
      productImageRepo.deleteAllInBatch(currentImages);
      return;
    }

    List<Long> requestedImageIds = images.stream()
        .filter(img -> img.imageId() != null)
        .map(ImageRequest::imageId)
        .toList();

    Map<Long, ProductImage> existingImageMap;
    if (!requestedImageIds.isEmpty()) {
      List<ProductImage> foundImages = productImageRepo.findByProductIdAndIdIn(
          product.getId(), requestedImageIds);
      if (foundImages.size() != requestedImageIds.size()) {
        throw new BusinessException(ErrorCode.PROD_INVALID_IMAGE_ID);
      }
      existingImageMap = foundImages.stream()
          .collect(Collectors.toMap(ProductImage::getId, img -> img));
    } else {
      existingImageMap = Map.of();
    }

    Set<Integer> sortOrders = new HashSet<>();
    for (ImageRequest img : images) {
      if (!sortOrders.add(img.sortOrder())) {
        throw new BusinessException(ErrorCode.PROD_DUPLICATE_SORT_ORDER);
      }
    }

    // 요청에 없는 기존 이미지만 삭제
    Set<Long> keptImageIds = new HashSet<>(requestedImageIds);
    List<ProductImage> toDelete = currentImages.stream()
        .filter(img -> !keptImageIds.contains(img.getId()))
        .toList();
    recordDeletedUrls(toDelete);
    productImageRepo.deleteAllInBatch(toDelete);

    // 기존 이미지는 sortOrder만 업데이트, 새 이미지만 insert
    List<ProductImage> newImages = new ArrayList<>();
    for (ImageRequest img : images) {
      if (img.imageId() != null) {
        existingImageMap.get(img.imageId()).updateSortOrder(img.sortOrder());
      } else {
        String url = imageService.validateAndGetProductUrl(img.objectKey());
        objectKeysToMove.add(img.objectKey());
        newImages.add(ProductImage.builder()
            .product(product)
            .url(url)
            .sortOrder(img.sortOrder())
            .build());
      }
    }
    if (!newImages.isEmpty()) {
      productImageRepo.saveAll(newImages);
    }
  }

  private void recordDeletedUrls(List<ProductImage> images) {
    List<DeletedProductImage> deleted = images.stream()
        .map(img -> DeletedProductImage.builder().url(img.getUrl()).build())
        .toList();
    deletedProductImageRepo.saveAll(deleted);
  }

  private void processDetail(Product product, String detail) {
    Optional<ProductDetail> existing = productDetailRepo.findFirstByProductId(product.getId());
    if (detail.isEmpty()) {
      existing.ifPresent(productDetailRepo::delete);
      return;
    }
    String sanitized = Jsoup.clean(detail, Safelist.relaxed());
    if (existing.isPresent()) {
      existing.get().updateContent(sanitized);
    } else {
      productDetailRepo.save(ProductDetail.builder()
          .product(product)
          .content(sanitized)
          .build());
    }
  }

  private void validateRequest(ProductUpdateRequest request) {
    if (request.price() != null && request.price() < 0) {
      throw new BusinessException(ErrorCode.PROD_INVALID_PRICE);
    }
    if (request.discountRate() != null
        && (request.discountRate() < 0 || request.discountRate() > 100)) {
      throw new BusinessException(ErrorCode.PROD_INVALID_DISCOUNT_RATE);
    }
    if (request.status() != null) {
      boolean valid = Arrays.stream(SaleStatus.values())
          .anyMatch(s -> s.name().equals(request.status()));
      if (!valid) {
        throw new BusinessException(ErrorCode.BAD_REQUEST);
      }
    }
    if (request.images() != null) {
      for (ImageRequest img : request.images()) {
        boolean hasImageId = img.imageId() != null;
        boolean hasObjectKey = img.objectKey() != null && !img.objectKey().isBlank();
        if (hasImageId == hasObjectKey) {
          throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
      }
    }
  }

  private Store resolveStore(Long authId) {
    Seller seller = sellerRepo.findByAuthId(authId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));
    return storeRepo.findBySeller(seller)
        .orElseThrow(() -> new BusinessException(ErrorCode.PROD_STORE_NOT_FOUND));
  }

  private Product findOwnProduct(Long productId, Store store) {
    Product product = productRepo.findById(productId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PROD_NOT_FOUND));
    if (product.getDeletedAt() != null) {
      throw new BusinessException(ErrorCode.PROD_DELETED);
    }
    if (!product.getStore().getId().equals(store.getId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return product;
  }
}
