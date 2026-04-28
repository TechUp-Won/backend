package com.example.WonkaoTalk.domain.product.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.WonkaoTalk.config.TestContainerConfig;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailResponse;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductDetail;
import com.example.WonkaoTalk.domain.product.entity.ProductImage;
import com.example.WonkaoTalk.domain.product.entity.ProductOption;
import com.example.WonkaoTalk.domain.product.entity.ProductOptionGroup;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.entity.VariantOptionMap;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.service.ProductService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
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
class ProductDetailIntegrationTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private ProductService productService;

  private Category category;
  private Product product;

  @BeforeEach
  void setUp() {
    category = saveCategory();
    product = saveProduct(category);
    em.flush();
    em.clear();
  }

  @Test
  @DisplayName("이미지가 sortOrder 오름차순으로 반환된다")
  void images_returnedInSortOrderAscending() {
    saveImage(product, "https://cdn.example.com/2.jpg", 2);
    saveImage(product, "https://cdn.example.com/1.jpg", 1);
    saveImage(product, "https://cdn.example.com/3.jpg", 3);
    em.flush();
    em.clear();

    ProductDetailResponse response = productService.getProductDetail(product.getId());

    assertThat(response.getImages()).hasSize(3);
    assertThat(response.getImages().get(0).getSortOrder()).isEqualTo(1);
    assertThat(response.getImages().get(1).getSortOrder()).isEqualTo(2);
    assertThat(response.getImages().get(2).getSortOrder()).isEqualTo(3);
  }

  @Test
  @DisplayName("옵션 그룹과 옵션이 계층 구조로 반환된다")
  void optionGroups_containNestedOptions() {
    ProductOptionGroup colorGroup = saveOptionGroup(product, "색상");
    saveOption(colorGroup, "화이트");
    saveOption(colorGroup, "네이비");

    ProductOptionGroup sizeGroup = saveOptionGroup(product, "사이즈");
    saveOption(sizeGroup, "M");
    em.flush();
    em.clear();

    ProductDetailResponse response = productService.getProductDetail(product.getId());

    assertThat(response.getOptionGroups()).hasSize(2);
    ProductDetailResponse.OptionGroupInfo color = response.getOptionGroups().stream()
        .filter(g -> g.getName().equals("색상")).findFirst().orElseThrow();
    assertThat(color.getOptions()).hasSize(2);
    assertThat(color.getOptions().stream().map(ProductDetailResponse.OptionInfo::getName))
        .containsExactlyInAnyOrder("화이트", "네이비");
  }

  @Test
  @DisplayName("variant의 combinationIds에 연결된 optionId 목록이 포함된다")
  void variants_containCombinationIds() {
    ProductOptionGroup group = saveOptionGroup(product, "색상");
    ProductOption white = saveOption(group, "화이트");
    ProductOption navy = saveOption(group, "네이비");

    ProductVariant variant = saveVariant(product, "화이트 / M", 50);
    saveVariantOptionMap(variant, white);
    saveVariantOptionMap(variant, navy);
    em.flush();
    em.clear();

    ProductDetailResponse response = productService.getProductDetail(product.getId());

    assertThat(response.getVariants()).hasSize(1);
    assertThat(response.getVariants().get(0).getCombinationIds())
        .containsExactlyInAnyOrder(white.getId(), navy.getId());
  }

  @Test
  @DisplayName("상세 내용이 없으면 detail이 null이다")
  void detail_isNullWhenNotExists() {
    ProductDetailResponse response = productService.getProductDetail(product.getId());

    assertThat(response.getDetail()).isNull();
  }

  @Test
  @DisplayName("상세 내용이 있으면 content가 반환된다")
  void detail_returnsContent_whenExists() {
    saveDetail(product, "<p>상품 상세 내용</p>");
    em.flush();
    em.clear();

    ProductDetailResponse response = productService.getProductDetail(product.getId());

    assertThat(response.getDetail()).isNotNull();
    assertThat(response.getDetail().getContent()).isEqualTo("<p>상품 상세 내용</p>");
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

  private Category saveCategory() {
    Category cat = new Category();
    ReflectionTestUtils.setField(cat, "name", "테스트카테고리");
    ReflectionTestUtils.setField(cat, "depth", 1);
    em.persist(cat);
    return cat;
  }

  private Product saveProduct(Category cat) {
    Product p = new Product();
    ReflectionTestUtils.setField(p, "storeId", 1L);
    ReflectionTestUtils.setField(p, "name", "테스트상품");
    ReflectionTestUtils.setField(p, "category", cat);
    ReflectionTestUtils.setField(p, "price", 10000);
    ReflectionTestUtils.setField(p, "likeCount", 0);
    ReflectionTestUtils.setField(p, "status", SaleStatus.ON_SALE);
    ReflectionTestUtils.setField(p, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(p, "updatedAt", LocalDateTime.now());
    em.persist(p);
    return p;
  }

  private ProductImage saveImage(Product p, String url, int sortOrder) {
    ProductImage img = new ProductImage();
    ReflectionTestUtils.setField(img, "product", p);
    ReflectionTestUtils.setField(img, "url", url);
    ReflectionTestUtils.setField(img, "sortOrder", sortOrder);
    ReflectionTestUtils.setField(img, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(img, "updatedAt", LocalDateTime.now());
    em.persist(img);
    return img;
  }

  private ProductDetail saveDetail(Product p, String content) {
    ProductDetail detail = new ProductDetail();
    ReflectionTestUtils.setField(detail, "product", p);
    ReflectionTestUtils.setField(detail, "content", content);
    ReflectionTestUtils.setField(detail, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(detail, "updatedAt", LocalDateTime.now());
    em.persist(detail);
    return detail;
  }

  private ProductOptionGroup saveOptionGroup(Product p, String name) {
    ProductOptionGroup group = new ProductOptionGroup();
    ReflectionTestUtils.setField(group, "product", p);
    ReflectionTestUtils.setField(group, "name", name);
    ReflectionTestUtils.setField(group, "sortOrder", 1);
    ReflectionTestUtils.setField(group, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(group, "updatedAt", LocalDateTime.now());
    em.persist(group);
    return group;
  }

  private ProductOption saveOption(ProductOptionGroup group, String name) {
    ProductOption option = new ProductOption();
    ReflectionTestUtils.setField(option, "productOptionGroup", group);
    ReflectionTestUtils.setField(option, "name", name);
    em.persist(option);
    return option;
  }

  private ProductVariant saveVariant(Product p, String name, int stock) {
    ProductVariant variant = new ProductVariant();
    ReflectionTestUtils.setField(variant, "product", p);
    ReflectionTestUtils.setField(variant, "name", name);
    ReflectionTestUtils.setField(variant, "stock", stock);
    ReflectionTestUtils.setField(variant, "status", SaleStatus.ON_SALE);
    ReflectionTestUtils.setField(variant, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(variant, "updatedAt", LocalDateTime.now());
    em.persist(variant);
    return variant;
  }

  private void saveVariantOptionMap(ProductVariant variant, ProductOption option) {
    VariantOptionMap map = new VariantOptionMap();
    ReflectionTestUtils.setField(map, "productVariant", variant);
    ReflectionTestUtils.setField(map, "productOption", option);
    em.persist(map);
  }
}
