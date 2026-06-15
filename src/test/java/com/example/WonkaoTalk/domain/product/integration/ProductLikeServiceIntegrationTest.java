package com.example.WonkaoTalk.domain.product.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.config.TestContainerConfig;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.product.dto.ProductLikeListResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductLikeListResponse.LikeSummary;
import com.example.WonkaoTalk.domain.product.dto.ProductLikeToggleResponse;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductLike;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductLikeRepo;
import com.example.WonkaoTalk.domain.product.service.ProductLikeService;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.store.entity.Store;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.enums.Gender;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestContainerConfig.class)
@Transactional
class ProductLikeServiceIntegrationTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private ProductLikeService productLikeService;

  @Autowired
  private ProductLikeRepo productLikeRepo;

  private User testUser;
  private Long testUserId;
  private Product product;

  @BeforeEach
  void setUp() {
    testUser = saveUser("테스트유저");
    testUserId = testUser.getId();
    Category category = saveCategory();
    Store store = saveStore();
    product = saveProduct(store, category);
    em.flush();
    em.clear();
  }

  // ── toggle ───────────────────────────────────────────────────────────────

  @Test
  @DisplayName("좋아요 기록이 없으면 ProductLike가 생성되고 likeCount가 증가한다")
  void toggle_createsLike_andIncreasesLikeCount() {
    ProductLikeToggleResponse response = productLikeService.toggle(testUserId, product.getId());

    assertThat(response.isLiked()).isTrue();
    assertThat(response.getLikeCount()).isEqualTo(1);

    em.flush();
    em.clear();

    assertThat(productLikeRepo.existsByProductIdAndUserId(product.getId(), testUserId)).isTrue();
    Product updated = em.find(Product.class, product.getId());
    assertThat(updated.getLikeCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("좋아요 기록이 있으면 ProductLike가 삭제되고 likeCount가 감소한다")
  void toggle_removesLike_andDecreasesLikeCount() {
    productLikeService.toggle(testUserId, product.getId());
    em.flush();
    em.clear();

    Product reloaded = em.find(Product.class, product.getId());
    ProductLikeToggleResponse response = productLikeService.toggle(testUserId, reloaded.getId());

    assertThat(response.isLiked()).isFalse();
    assertThat(response.getLikeCount()).isEqualTo(0);

    em.flush();
    em.clear();

    assertThat(productLikeRepo.existsByProductIdAndUserId(product.getId(), testUserId)).isFalse();
    Product updated = em.find(Product.class, product.getId());
    assertThat(updated.getLikeCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("존재하지 않는 상품이면 PROD_NOT_FOUND를 던진다")
  void toggle_throwsNotFound_whenProductNotExists() {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> productLikeService.toggle(testUserId, 999999L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_NOT_FOUND);
  }

  @Test
  @DisplayName("삭제된 상품이면 PROD_DELETED를 던진다")
  void toggle_throwsDeleted_whenProductDeleted() {
    ReflectionTestUtils.setField(product, "deletedAt", LocalDateTime.now());
    em.merge(product);
    em.flush();
    em.clear();

    BusinessException ex = assertThrows(BusinessException.class,
        () -> productLikeService.toggle(testUserId, product.getId()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROD_DELETED);
  }

  // ── getLikedProducts ─────────────────────────────────────────────────────

  @Test
  @DisplayName("좋아요한 상품 목록을 createdAt 내림차순으로 반환한다")
  void getLikedProducts_returnsLikesOrderedByCreatedAtDesc() {
    Category category = saveCategory();
    Store store = saveStore();
    Product productB = saveProduct(store, category);

    saveLike(testUser, product, LocalDateTime.now().minusDays(1));
    saveLike(testUser, productB, LocalDateTime.now());
    em.flush();
    em.clear();

    Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
    ProductLikeListResponse response = productLikeService.getLikedProducts(testUserId, pageable);

    assertThat(response.likes()).hasSize(2);
    assertThat(response.likes().get(0).productId()).isEqualTo(productB.getId());
    assertThat(response.likes().get(1).productId()).isEqualTo(product.getId());
    assertThat(response.pageInfo().totalElements()).isEqualTo(2);
  }

  @Test
  @DisplayName("좋아요한 상품의 LikeSummary 필드가 올바르게 매핑된다")
  void getLikedProducts_mapsLikeSummaryFieldsCorrectly() {
    LocalDateTime likedAt = LocalDateTime.now();
    saveLike(testUser, product, likedAt);
    em.flush();
    em.clear();

    Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
    ProductLikeListResponse response = productLikeService.getLikedProducts(testUserId, pageable);

    LikeSummary summary = response.likes().get(0);
    assertThat(summary.productId()).isEqualTo(product.getId());
    assertThat(summary.name()).isEqualTo("테스트상품");
    assertThat(summary.price()).isEqualTo(10000);
    assertThat(summary.discountedPrice()).isEqualTo(10000);
    assertThat(summary.likedAt()).isCloseTo(likedAt, within(1, ChronoUnit.SECONDS));
  }

  @Test
  @DisplayName("좋아요한 상품이 없으면 빈 목록을 반환한다")
  void getLikedProducts_returnsEmpty_whenNoLikes() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
    ProductLikeListResponse response = productLikeService.getLikedProducts(testUserId, pageable);

    assertThat(response.likes()).isEmpty();
    assertThat(response.pageInfo().totalElements()).isEqualTo(0);
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

  private int userSeq = 0;

  private User saveUser(String nickname) {
    Auth auth = Auth.builder().build();
    em.persist(auth);
    User user = User.builder()
        .nickname(nickname)
        .name("테스트")
        .phone(String.format("010-%04d-%04d", ++userSeq, userSeq))
        .gender(Gender.NONE)
        .auth(auth)
        .build();
    em.persist(user);
    return user;
  }

  private Category saveCategory() {
    Category cat = new Category();
    ReflectionTestUtils.setField(cat, "name", "테스트카테고리");
    ReflectionTestUtils.setField(cat, "depth", 1);
    em.persist(cat);
    return cat;
  }

  private Store saveStore() {
    Auth auth = Auth.builder().build();
    em.persist(auth);

    Seller seller = Seller.builder()
        .buzNo(String.valueOf(System.nanoTime()).substring(0, 10))
        .name("테스트판매자")
        .phone("010-0000-0000")
        .auth(auth)
        .build();
    em.persist(seller);

    Store store = Store.builder()
        .name("테스트스토어")
        .description("설명")
        .phone("010-0000-0000")
        .seller(seller)
        .build();
    em.persist(store);
    return store;
  }

  private Product saveProduct(Store store, Category cat) {
    Product p = new Product();
    ReflectionTestUtils.setField(p, "store", store);
    ReflectionTestUtils.setField(p, "name", "테스트상품");
    ReflectionTestUtils.setField(p, "category", cat);
    ReflectionTestUtils.setField(p, "price", 10000);
    ReflectionTestUtils.setField(p, "discountedPrice", 10000);
    ReflectionTestUtils.setField(p, "likeCount", 0);
    ReflectionTestUtils.setField(p, "status", SaleStatus.ON_SALE);
    ReflectionTestUtils.setField(p, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(p, "updatedAt", LocalDateTime.now());
    em.persist(p);
    return p;
  }

  private ProductLike saveLike(User user, Product product, LocalDateTime createdAt) {
    ProductLike like = ProductLike.builder()
        .product(product)
        .userId(user.getId())
        .build();
    em.persist(like);
    em.flush();
    em.createNativeQuery("UPDATE product_likes SET created_at = :createdAt WHERE id = :id")
        .setParameter("createdAt", createdAt)
        .setParameter("id", like.getId())
        .executeUpdate();
    return like;
  }
}
