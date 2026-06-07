package com.example.WonkaoTalk.domain.search.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.WonkaoTalk.config.SearchTestContainerConfig;
import com.example.WonkaoTalk.config.TestContainerConfig;
import com.example.WonkaoTalk.domain.product.enums.ProductSortType;
import com.example.WonkaoTalk.domain.search.document.ProductDocument;
import com.example.WonkaoTalk.domain.search.repo.ProductSearchQueryRepository;
import com.example.WonkaoTalk.domain.search.repo.ProductSearchResult;
import com.example.WonkaoTalk.domain.search.repo.ProductSearchRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * ES + nori 기반 상품 검색의 핵심(옵션값 매칭)을 실제 ElasticSearch 컨테이너로 검증한다.
 * Docker 가 없는 환경에서는 자동으로 비활성화된다(disabledWithoutDocker).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Import({TestContainerConfig.class, SearchTestContainerConfig.class})
@TestPropertySource(properties = "search.index.auto-create=false")
class ProductSearchQueryRepositoryImplTest {

  @Autowired
  private ElasticsearchOperations operations;

  @Autowired
  private ProductSearchRepository searchRepository;

  @Autowired
  private ProductSearchQueryRepository queryRepository;

  private IndexOperations indexOps;

  @BeforeEach
  void setUp() {
    indexOps = operations.indexOps(ProductDocument.class);
    if (indexOps.exists()) {
      indexOps.delete();
    }
    indexOps.createWithMapping();
  }

  @Test
  @DisplayName("'흰색 티셔츠' 검색 시 상품명 '티셔츠' + 옵션 '흰색' 상품이 매칭된다")
  void matchesProductByNameAndOptionValue() {
    index(1L, "티셔츠", List.of("흰색", "검정"), 1L, 10000, 5);
    index(2L, "티셔츠", List.of("빨강", "파랑"), 1L, 10000, 99);
    index(3L, "셔츠", List.of("흰색"), 1L, 10000, 50);
    refresh();

    ProductSearchResult result = queryRepository.search(
        "흰색 티셔츠", null, null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result.ids()).containsExactly(1L);
  }

  @Test
  @DisplayName("옵션값 단일 토큰으로도 매칭된다")
  void matchesByOptionValueOnly() {
    index(1L, "후드티", List.of("아이보리"), 1L, 20000, 3);
    index(2L, "후드티", List.of("블랙"), 1L, 20000, 7);
    refresh();

    ProductSearchResult result = queryRepository.search(
        "아이보리", null, null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result.ids()).containsExactly(1L);
  }

  @Test
  @DisplayName("상품명의 부분 문자열('셔츠'⊂'티셔츠')로도 매칭된다 — ngram 부분매칭")
  void matchesByPartialSubstring() {
    index(1L, "티셔츠", List.of("흰색"), 1L, 10000, 5);
    index(2L, "원피스", List.of("블루"), 1L, 10000, 50);
    refresh();

    ProductSearchResult result = queryRepository.search(
        "셔츠", null, null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result.ids()).containsExactly(1L);
  }

  @Test
  @DisplayName("부분 문자열이라도 어디에도 없으면 매칭되지 않는다")
  void excludesProduct_whenSubstringAbsent() {
    index(1L, "티셔츠", List.of("흰색"), 1L, 10000, 5);
    refresh();

    ProductSearchResult result = queryRepository.search(
        "바지", null, null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result.ids()).isEmpty();
  }

  @Test
  @DisplayName("모든 토큰이 매칭되지 않으면(AND) 결과에서 제외된다")
  void excludesProduct_whenNotAllTokensMatch() {
    // '흰색'은 매칭되나 '바지'는 상품명/옵션 어디에도 없음
    index(1L, "티셔츠", List.of("흰색"), 1L, 10000, 5);
    refresh();

    ProductSearchResult result = queryRepository.search(
        "흰색 바지", null, null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result.ids()).isEmpty();
  }

  @Test
  @DisplayName("POPULAR 정렬 시 likeCount 내림차순으로 정렬된다")
  void sortsByLikeCountDesc_whenPopular() {
    index(1L, "티셔츠", List.of("흰색"), 1L, 10000, 10);
    index(2L, "반팔 티셔츠", List.of("흰색"), 1L, 10000, 80);
    index(3L, "긴팔 티셔츠", List.of("흰색"), 1L, 10000, 40);
    refresh();

    ProductSearchResult result = queryRepository.search(
        "티셔츠", null, null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result.ids()).containsExactly(2L, 3L, 1L);
  }

  @Test
  @DisplayName("categoryIds 필터에 해당하는 카테고리의 상품만 조회된다")
  void appliesCategoryFilter() {
    index(1L, "티셔츠", List.of("흰색"), 1L, 10000, 5);
    index(2L, "티셔츠", List.of("흰색"), 2L, 10000, 5);
    refresh();

    ProductSearchResult result = queryRepository.search(
        "티셔츠", List.of(1L), null, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result.ids()).containsExactly(1L);
  }

  @Test
  @DisplayName("PRICE_ASC 정렬 시 discountedPrice 오름차순으로 정렬된다")
  void sortsByPriceAsc_whenPriceAsc() {
    index(1L, "티셔츠", List.of("흰색"), 1L, 30000, 5);
    index(2L, "티셔츠", List.of("흰색"), 1L, 10000, 5);
    index(3L, "티셔츠", List.of("흰색"), 1L, 20000, 5);
    refresh();

    ProductSearchResult result = queryRepository.search(
        "티셔츠", null, null, null, ProductSortType.PRICE_ASC, null, null, 10);

    assertThat(result.ids()).containsExactly(2L, 3L, 1L);
  }

  @Test
  @DisplayName("LATEST 정렬 시 createdAt 내림차순으로 정렬된다")
  void sortsByCreatedAtDesc_whenLatest() {
    // index 헬퍼가 createdAt = 1_700_000_000_000 + id 로 부여 → id 가 클수록 최신
    index(1L, "티셔츠", List.of("흰색"), 1L, 10000, 5);
    index(2L, "티셔츠", List.of("흰색"), 1L, 10000, 5);
    index(3L, "티셔츠", List.of("흰색"), 1L, 10000, 5);
    refresh();

    ProductSearchResult result = queryRepository.search(
        "티셔츠", null, null, null, ProductSortType.LATEST, null, null, 10);

    assertThat(result.ids()).containsExactly(3L, 2L, 1L);
  }

  @Test
  @DisplayName("가격 필터(discountedPrice)가 적용된다")
  void appliesPriceFilter() {
    index(1L, "티셔츠", List.of("흰색"), 1L, 8000, 5);
    index(2L, "티셔츠", List.of("흰색"), 1L, 20000, 5);
    refresh();

    ProductSearchResult result = queryRepository.search(
        "티셔츠", null, 10000, null, ProductSortType.POPULAR, null, null, 10);

    assertThat(result.ids()).containsExactly(2L);
  }

  @Test
  @DisplayName("size+1 조회로 hasNext 와 다음 커서를 산출한다")
  void detectsHasNextAndCursor() {
    index(1L, "티셔츠", List.of("흰색"), 1L, 10000, 30);
    index(2L, "티셔츠", List.of("흰색"), 1L, 10000, 20);
    index(3L, "티셔츠", List.of("흰색"), 1L, 10000, 10);
    refresh();

    ProductSearchResult page = queryRepository.search(
        "티셔츠", null, null, null, ProductSortType.POPULAR, null, null, 2);

    assertThat(page.ids()).containsExactly(1L, 2L);
    assertThat(page.hasNext()).isTrue();
    assertThat(page.nextCursorId()).isEqualTo(2L);
    assertThat(page.nextCursorSortValue()).isEqualTo(20L);

    // 커서 이후 페이지
    ProductSearchResult next = queryRepository.search(
        "티셔츠", null, null, null, ProductSortType.POPULAR,
        page.nextCursorId(), page.nextCursorSortValue(), 2);

    assertThat(next.ids()).containsExactly(3L);
    assertThat(next.hasNext()).isFalse();
  }

  private void index(Long id, String name, List<String> optionValues, Long categoryId,
      Integer discountedPrice, Integer likeCount) {
    searchRepository.save(ProductDocument.builder()
        .id(id)
        .name(name)
        .optionValues(optionValues)
        .categoryId(categoryId)
        .discountedPrice(discountedPrice)
        .likeCount(likeCount)
        .createdAt(1_700_000_000_000L + id)
        .status("ON_SALE")
        .build());
  }

  private void refresh() {
    indexOps.refresh();
  }
}
