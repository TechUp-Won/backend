package com.example.WonkaoTalk.domain.product.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.image.service.ImageService;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateRequest.ImageRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateRequest.OptionGroupRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateRequest.VariantRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateResponse;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductDetail;
import com.example.WonkaoTalk.domain.product.entity.ProductImage;
import com.example.WonkaoTalk.domain.product.entity.ProductOption;
import com.example.WonkaoTalk.domain.product.entity.ProductOptionGroup;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.entity.StockHistory;
import com.example.WonkaoTalk.domain.product.entity.VariantOptionMap;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.enums.StockChangeReason;
import com.example.WonkaoTalk.domain.product.event.ProductCreatedEvent;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class ProductCreateService {

  private final SellerRepo sellerRepo;
  private final StoreRepo storeRepo;
  private final CategoryRepo categoryRepo;
  private final ProductRepo productRepo;
  private final ProductDetailRepo productDetailRepo;
  private final ProductImageRepo productImageRepo;
  private final ProductOptionGroupRepo productOptionGroupRepo;
  private final ProductOptionRepo productOptionRepo;
  private final ProductVariantRepo productVariantRepo;
  private final VariantOptionMapRepo variantOptionMapRepo;
  private final StockHistoryRepo stockHistoryRepo;
  private final ImageService imageService;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public ProductCreateResponse create(Long authId, ProductCreateRequest request) {
    Seller seller = sellerRepo.findByAuthId(authId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));
    Store store = storeRepo.findBySeller(seller)
        .orElseThrow(() -> new BusinessException(ErrorCode.PROD_STORE_NOT_FOUND));
    Category category = categoryRepo.findById(request.categoryId())
        .orElseThrow(() -> new BusinessException(ErrorCode.PROD_CATEGORY_NOT_FOUND));

    validatePrice(request);
    validateOptionVariantConsistency(request);
    validateSortOrders(request);
    validateOptionNames(request);

    boolean hasOptions = hasOptions(request);
    int discountRate = request.discountRate() != null ? request.discountRate() : 0;
    int discountedPrice = (int) Math.round(request.price() * (1 - discountRate / 100.0));

    List<String> objectKeysToMove = new ArrayList<>();

    String thumbnailUrl = null;
    if (request.thumbnailKey() != null) {
      thumbnailUrl = imageService.validateAndGetProductUrl(request.thumbnailKey());
      objectKeysToMove.add(request.thumbnailKey());
    }

    List<String> imageUrls = new ArrayList<>();
    if (request.images() != null) {
      for (ImageRequest img : request.images()) {
        imageUrls.add(imageService.validateAndGetProductUrl(img.objectKey()));
        objectKeysToMove.add(img.objectKey());
      }
    }

    Product product = Product.builder()
        .store(store)
        .name(request.name())
        .thumbnail(thumbnailUrl)
        .category(category)
        .discountRate(discountRate)
        .price(request.price())
        .discountedPrice(discountedPrice)
        .status(SaleStatus.ON_SALE)
        .build();
    productRepo.save(product);

    if (request.detail() != null) {
      String sanitized = Jsoup.clean(request.detail(), Safelist.relaxed());
      productDetailRepo.save(ProductDetail.builder()
          .product(product)
          .content(sanitized)
          .build());
    }

    if (request.images() != null) {
      for (int i = 0; i < request.images().size(); i++) {
        ImageRequest imgReq = request.images().get(i);
        productImageRepo.save(ProductImage.builder()
            .product(product)
            .url(imageUrls.get(i))
            .sortOrder(imgReq.sortOrder())
            .build());
      }
    }

    if (!hasOptions) {
      ProductVariant variant = productVariantRepo.save(ProductVariant.builder()
          .product(product)
          .name("기본")
          .stock(request.stock())
          .status(SaleStatus.ON_SALE)
          .build());
      stockHistoryRepo.save(
          StockHistory.of(variant, null, request.stock(), 0, StockChangeReason.INITIAL_STOCK));
    } else {
      saveOptionsAndVariants(product, request);
    }

    if (!objectKeysToMove.isEmpty()) {
      eventPublisher.publishEvent(new ProductCreatedEvent(objectKeysToMove));
    }

    return new ProductCreateResponse(
        product.getId(),
        store.getId(),
        product.getName(),
        category.getId(),
        product.getThumbnail(),
        product.getPrice(),
        product.getDiscountRate(),
        product.getDiscountedPrice(),
        product.getStatus().name(),
        product.getCreatedAt()
    );
  }

  private void saveOptionsAndVariants(Product product, ProductCreateRequest request) {
    List<OptionGroupRequest> sortedGroups = request.optionGroups().stream()
        .sorted((a, b) -> a.sortOrder().compareTo(b.sortOrder()))
        .toList();

    // groupIndex → Map<optionName, ProductOption>
    List<Map<String, ProductOption>> groupOptionMaps = new ArrayList<>();

    for (OptionGroupRequest groupReq : sortedGroups) {
      ProductOptionGroup group = productOptionGroupRepo.save(ProductOptionGroup.builder()
          .product(product)
          .name(groupReq.name())
          .sortOrder(groupReq.sortOrder())
          .build());

      Map<String, ProductOption> optionMap = new LinkedHashMap<>();
      for (var optReq : groupReq.options()) {
        ProductOption option = productOptionRepo.save(ProductOption.builder()
            .productOptionGroup(group)
            .name(optReq.name())
            .build());
        optionMap.put(optReq.name(), option);
      }
      groupOptionMaps.add(optionMap);
    }

    Set<List<String>> seenCombinations = new HashSet<>();

    for (VariantRequest variantReq : request.variants()) {
      List<String> combo = variantReq.optionNames();
      if (!seenCombinations.add(combo)) {
        throw new BusinessException(ErrorCode.PROD_MISMATCH_VARIANT_OPTION);
      }

      String variantName = String.join(" / ", combo);

      ProductVariant variant = productVariantRepo.save(ProductVariant.builder()
          .product(product)
          .name(variantName)
          .stock(variantReq.stock())
          .status(SaleStatus.ON_SALE)
          .build());
      stockHistoryRepo.save(
          StockHistory.of(variant, null, variantReq.stock(), 0, StockChangeReason.INITIAL_STOCK));

      for (int i = 0; i < combo.size(); i++) {
        ProductOption option = groupOptionMaps.get(i).get(combo.get(i));
        variantOptionMapRepo.save(VariantOptionMap.builder()
            .productVariant(variant)
            .productOption(option)
            .build());
      }
    }
  }

  private void validatePrice(ProductCreateRequest request) {
    if (request.price() < 0) {
      throw new BusinessException(ErrorCode.PROD_INVALID_PRICE);
    }
    if (request.discountRate() != null && (request.discountRate() < 0
        || request.discountRate() > 100)) {
      throw new BusinessException(ErrorCode.PROD_INVALID_DISCOUNT_RATE);
    }
  }

  private void validateOptionVariantConsistency(ProductCreateRequest request) {
    boolean hasOptionGroups = request.optionGroups() != null && !request.optionGroups().isEmpty();
    boolean hasVariants = request.variants() != null && !request.variants().isEmpty();

    if (hasOptionGroups != hasVariants) {
      throw new BusinessException(ErrorCode.PROD_MISMATCH_VARIANT_OPTION);
    }
    if (hasOptionGroups && request.stock() != null) {
      throw new BusinessException(ErrorCode.PROD_INVALID_STOCK_OPTION);
    }
    if (!hasOptionGroups && (request.stock() == null || request.stock() < 1)) {
      throw new BusinessException(ErrorCode.PROD_INVALID_QUANTITY);
    }
    if (hasVariants) {
      for (VariantRequest variant : request.variants()) {
        if (variant.stock() == null || variant.stock() < 1) {
          throw new BusinessException(ErrorCode.PROD_INVALID_QUANTITY);
        }
      }
    }
  }

  private void validateSortOrders(ProductCreateRequest request) {
    if (request.images() != null) {
      Set<Integer> seen = new HashSet<>();
      for (ImageRequest img : request.images()) {
        if (!seen.add(img.sortOrder())) {
          throw new BusinessException(ErrorCode.PROD_DUPLICATE_SORT_ORDER);
        }
      }
    }
    if (request.optionGroups() != null) {
      Set<Integer> seen = new HashSet<>();
      for (OptionGroupRequest group : request.optionGroups()) {
        if (!seen.add(group.sortOrder())) {
          throw new BusinessException(ErrorCode.PROD_DUPLICATE_SORT_ORDER);
        }
      }
    }
  }

  private void validateOptionNames(ProductCreateRequest request) {
    if (request.optionGroups() == null || request.optionGroups().isEmpty()) {
      return;
    }

    // 그룹명 중복 검사
    Set<String> groupNames = new HashSet<>();
    for (OptionGroupRequest group : request.optionGroups()) {
      if (!groupNames.add(group.name())) {
        throw new BusinessException(ErrorCode.PROD_MISMATCH_VARIANT_OPTION);
      }
      // 그룹 내 옵션명 중복 검사
      Set<String> optionNames = new HashSet<>();
      for (var opt : group.options()) {
        if (!optionNames.add(opt.name())) {
          throw new BusinessException(ErrorCode.PROD_MISMATCH_VARIANT_OPTION);
        }
      }
    }

    // sortOrder 오름차순 정렬된 그룹 리스트로 옵션 이름 셋 구성
    List<Set<String>> groupOptionSets = request.optionGroups().stream()
        .sorted((a, b) -> a.sortOrder().compareTo(b.sortOrder()))
        .map(g -> g.options().stream()
            .map(o -> o.name())
            .collect(Collectors.toSet()))
        .toList();

    int groupCount = groupOptionSets.size();

    for (VariantRequest variant : request.variants()) {
      if (variant.optionNames().size() != groupCount) {
        throw new BusinessException(ErrorCode.PROD_MISMATCH_VARIANT_OPTION);
      }
      for (int i = 0; i < groupCount; i++) {
        if (!groupOptionSets.get(i).contains(variant.optionNames().get(i))) {
          throw new BusinessException(ErrorCode.PROD_MISMATCH_VARIANT_OPTION);
        }
      }
    }
  }

  private boolean hasOptions(ProductCreateRequest request) {
    return request.optionGroups() != null && !request.optionGroups().isEmpty();
  }
}
