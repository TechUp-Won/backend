package com.example.WonkaoTalk.domain.product.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.product.dto.ProductLikeListResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductLikeListResponse.LikeSummary;
import com.example.WonkaoTalk.domain.product.dto.ProductLikeToggleResponse;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductLike;
import com.example.WonkaoTalk.domain.product.repo.ProductLikeRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.order.dto.PageInfoDto;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductLikeService {

  private final ProductRepo productRepo;
  private final ProductLikeRepo productLikeRepo;

  public ProductLikeToggleResponse toggle(Long userId, Long productId) {
    Product product = productRepo.findByIdWithLock(productId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PROD_NOT_FOUND));

    if (product.getDeletedAt() != null) {
      throw new BusinessException(ErrorCode.PROD_DELETED);
    }

    Optional<ProductLike> existing = productLikeRepo.findByProductIdAndUserId(productId, userId);

    boolean isLiked;
    if (existing.isPresent()) {
      productLikeRepo.delete(existing.get());
      product.decreaseLikeCount();
      isLiked = false;
    } else {
      productLikeRepo.save(ProductLike.builder().product(product).userId(userId).build());
      product.increaseLikeCount();
      isLiked = true;
    }

    return ProductLikeToggleResponse.builder()
        .productId(productId)
        .isLiked(isLiked)
        .likeCount(product.getLikeCount())
        .build();
  }

  @Transactional(readOnly = true)
  public ProductLikeListResponse getLikedProducts(Long userId, Pageable pageable) {
    Page<ProductLike> likePage = productLikeRepo.findByUserIdWithProduct(userId, pageable);

    List<LikeSummary> likes = likePage.getContent().stream()
        .map(like -> {
          Product product = like.getProduct();
          return new LikeSummary(
              product.getId(),
              product.getName(),
              product.getThumbnail(),
              product.getPrice(),
              product.getDiscountRate(),
              product.getDiscountedPrice(),
              product.getLikeCount(),
              like.getCreatedAt()
          );
        })
        .toList();

    return new ProductLikeListResponse(likes, PageInfoDto.from(likePage));
  }
}
