package com.example.WonkaoTalk.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.product.dto.ProductLikeListResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductLikeListResponse.LikeSummary;
import com.example.WonkaoTalk.domain.product.dto.ProductLikeToggleResponse;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductLike;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductLikeRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.store.entity.Store;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductLikeServiceTest {

  @Mock
  private ProductRepo productRepository;

  @Mock
  private ProductLikeRepo productLikeRepository;

  @InjectMocks
  private ProductLikeService productLikeService;

  // ── toggle ───────────────────────────────────────────────────────────────

  @Test
  @DisplayName("좋아요 기록이 없으면 추가하고 likeCount가 증가한다")
  void toggle_addsLike_whenNotExists() {
    Product product = mockProduct(1L, 10);
    when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(product));
    when(productLikeRepository.findByProductIdAndUserId(1L, 100L)).thenReturn(Optional.empty());

    ProductLikeToggleResponse response = productLikeService.toggle(100L, 1L);

    assertThat(response.isLiked()).isTrue();
    assertThat(response.getLikeCount()).isEqualTo(11);
    assertThat(response.getProductId()).isEqualTo(1L);
    verify(productLikeRepository, times(1)).save(any(ProductLike.class));
    verify(productLikeRepository, never()).delete(any());
  }

  @Test
  @DisplayName("좋아요 기록이 있으면 삭제하고 likeCount가 감소한다")
  void toggle_removesLike_whenExists() {
    Product product = mockProduct(1L, 10);
    ProductLike existing = ProductLike.builder()
        .id(1L)
        .product(product)
        .userId(100L)
        .build();
    when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(product));
    when(productLikeRepository.findByProductIdAndUserId(1L, 100L)).thenReturn(Optional.of(existing));

    ProductLikeToggleResponse response = productLikeService.toggle(100L, 1L);

    assertThat(response.isLiked()).isFalse();
    assertThat(response.getLikeCount()).isEqualTo(9);
    verify(productLikeRepository, times(1)).delete(existing);
    verify(productLikeRepository, never()).save(any());
  }

  @Test
  @DisplayName("존재하지 않는 상품이면 PROD_NOT_FOUND를 던진다")
  void toggle_throwsNotFound_whenProductNotExists() {
    when(productRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productLikeService.toggle(100L, 999L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_NOT_FOUND);
  }

  @Test
  @DisplayName("삭제된 상품이면 PROD_DELETED를 던진다")
  void toggle_throwsDeleted_whenProductDeleted() {
    Product product = mockProduct(1L, 10);
    markDeleted(product);
    when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(product));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productLikeService.toggle(100L, 1L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_DELETED);
  }

  // ── getLikedProducts ─────────────────────────────────────────────────────

  @Test
  @DisplayName("좋아요한 상품 목록과 페이지 정보를 반환한다")
  void getLikedProducts_returnsSummariesAndPageInfo() {
    Store store = Store.builder().name("스토어").description("설명").phone("010-0000-0000").build();
    Product product = Product.builder()
        .id(1L)
        .store(store)
        .name("상품A")
        .thumbnail("https://cdn.example.com/1.jpg")
        .price(10000)
        .discountRate(20)
        .discountedPrice(8000)
        .status(SaleStatus.ON_SALE)
        .likeCount(5)
        .build();

    LocalDateTime likedAt = LocalDateTime.now();
    ProductLike like = ProductLike.builder()
        .id(1L)
        .product(product)
        .userId(100L)
        .createdAt(likedAt)
        .build();

    Pageable pageable = PageRequest.of(0, 10);
    when(productLikeRepository.findByUserIdWithProduct(100L, pageable))
        .thenReturn(new PageImpl<>(List.of(like), pageable, 1));

    ProductLikeListResponse response = productLikeService.getLikedProducts(100L, pageable);

    assertThat(response.likes()).hasSize(1);
    LikeSummary summary = response.likes().get(0);
    assertThat(summary.productId()).isEqualTo(1L);
    assertThat(summary.name()).isEqualTo("상품A");
    assertThat(summary.likeCount()).isEqualTo(5);
    assertThat(summary.likedAt()).isEqualTo(likedAt);
    assertThat(response.pageInfo().totalElements()).isEqualTo(1);
    assertThat(response.pageInfo().hasNext()).isFalse();
  }

  @Test
  @DisplayName("좋아요한 상품이 없으면 빈 목록을 반환한다")
  void getLikedProducts_returnsEmpty_whenNoLikes() {
    Pageable pageable = PageRequest.of(0, 10);
    when(productLikeRepository.findByUserIdWithProduct(100L, pageable))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    ProductLikeListResponse response = productLikeService.getLikedProducts(100L, pageable);

    assertThat(response.likes()).isEmpty();
    assertThat(response.pageInfo().totalElements()).isEqualTo(0);
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────

  private Product mockProduct(Long id, int likeCount) {
    return Product.builder()
        .id(id)
        .name("상품")
        .price(10000)
        .discountedPrice(10000)
        .status(SaleStatus.ON_SALE)
        .likeCount(likeCount)
        .build();
  }

  private void markDeleted(Product product) {
    ReflectionTestUtils.setField(product, "deletedAt", LocalDateTime.now());
  }
}
