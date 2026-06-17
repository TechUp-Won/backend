package com.example.WonkaoTalk.domain.product.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailCacheDto;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailCacheDto.DetailInfo;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailCacheDto.ImageInfo;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailCacheDto.OptionGroupInfo;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailCacheDto.OptionInfo;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailCacheDto.VariantCacheInfo;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductOption;
import com.example.WonkaoTalk.domain.product.entity.ProductOptionGroup;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.repo.ProductDetailRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductImageRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionGroupRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import com.example.WonkaoTalk.domain.product.repo.VariantOptionMapRepo;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductCacheService {

  private final ProductRepo productRepo;
  private final ProductImageRepo productImageRepo;
  private final ProductDetailRepo productDetailRepo;
  private final ProductOptionGroupRepo productOptionGroupRepo;
  private final ProductOptionRepo productOptionRepo;
  private final ProductVariantRepo productVariantRepo;
  private final VariantOptionMapRepo variantOptionMapRepo;

  @Cacheable(value = "productDetail", key = "#productId")
  public ProductDetailCacheDto getProductDetailBase(Long productId) {
    Product product = productRepo.findWithStoreById(productId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PROD_NOT_FOUND));

    if (product.getDeletedAt() != null) {
      throw new BusinessException(ErrorCode.PROD_DELETED);
    }

    List<ImageInfo> images = productImageRepo.findByProductIdOrderBySortOrderAsc(productId)
        .stream()
        .map(img -> ImageInfo.builder()
            .url(img.getUrl())
            .sortOrder(img.getSortOrder())
            .build())
        .toList();

    DetailInfo detail = productDetailRepo.findFirstByProductId(productId)
        .map(d -> DetailInfo.builder().content(d.getContent()).build())
        .orElse(null);

    List<ProductOptionGroup> groups = productOptionGroupRepo.findByProductId(productId);
    List<Long> groupIds = groups.stream().map(ProductOptionGroup::getId).toList();
    Map<Long, List<ProductOption>> optionsByGroup = productOptionRepo
        .findByProductOptionGroupIdIn(groupIds)
        .stream()
        .collect(Collectors.groupingBy(opt -> opt.getProductOptionGroup().getId()));

    List<OptionGroupInfo> optionGroups = groups.stream()
        .map(group -> toOptionGroupInfo(group, optionsByGroup))
        .toList();

    List<ProductVariant> variantList = productVariantRepo.findByProductId(productId);
    List<Long> variantIds = variantList.stream().map(ProductVariant::getId).toList();
    Map<Long, List<Long>> combinationIdsByVariant = variantOptionMapRepo
        .findByProductVariantIdIn(variantIds)
        .stream()
        .collect(Collectors.groupingBy(
            map -> map.getProductVariant().getId(),
            Collectors.mapping(map -> map.getProductOption().getId(), Collectors.toList())
        ));

    List<VariantCacheInfo> variants = variantList.stream()
        .map(variant -> VariantCacheInfo.builder()
            .variantId(variant.getId())
            .variantName(variant.getName())
            .combinationIds(combinationIdsByVariant.getOrDefault(variant.getId(), List.of()))
            .status(variant.getStatus().name())
            .build())
        .toList();

    return ProductDetailCacheDto.builder()
        .productId(product.getId())
        .productName(product.getName())
        .price(product.getPrice())
        .discountedPrice(product.getDiscountedPrice())
        .discountRate(product.getDiscountRate())
        .status(product.getStatus().name())
        .likeCount(product.getLikeCount())
        .isLiked(false)
        .store(ProductDetailCacheDto.StoreInfo.builder()
            .storeId(product.getStore().getId())
            .storeName(product.getStore().getName())
            .build())
        .images(images)
        .detail(detail)
        .optionGroups(optionGroups)
        .variants(variants)
        .build();
  }

  private OptionGroupInfo toOptionGroupInfo(ProductOptionGroup group,
      Map<Long, List<ProductOption>> optionsByGroup) {
    List<OptionInfo> options = optionsByGroup.getOrDefault(group.getId(), List.of())
        .stream()
        .map(opt -> OptionInfo.builder()
            .productOptionId(opt.getId())
            .name(opt.getName())
            .build())
        .toList();

    return OptionGroupInfo.builder()
        .productOptionGroupId(group.getId())
        .name(group.getName())
        .options(options)
        .build();
  }
}
