package com.example.WonkaoTalk.domain.search.indexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.test.util.ReflectionTestUtils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.search.document.ProductDocument;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * 전체 재색인 배치 단위 테스트 — 청크 반복, 옵션값 그룹핑, bulk 저장을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductReindexServiceTest {

  @Mock
  private ProductRepo productRepo;

  @Mock
  private ProductOptionRepo productOptionRepo;

  @Mock
  private ElasticsearchOperations operations;

  @Mock
  private ElasticsearchClient esClient;

  @InjectMocks
  private ProductReindexService reindexService;

  @Captor
  private ArgumentCaptor<List<ProductDocument>> docsCaptor;

  @Test
  @DisplayName("모든 청크를 색인하고 상품별 옵션값을 그룹핑한다")
  void reindexAll_indexesBatch_andGroupsOptionsByProduct() {
    Product p1 = product(1L);
    Product p2 = product(2L);
    // 첫 호출에 1건짜리 청크, 다음 호출에 빈 청크 → 루프 종료
    when(productRepo.findIndexableForReindex(any(), any(), any()))
        .thenReturn(List.of(p1, p2))
        .thenReturn(List.of());
    when(productOptionRepo.findOptionNamesByProductIds(List.of(1L, 2L)))
        .thenReturn(List.of(
            new Object[]{1L, "흰색"},
            new Object[]{1L, "검정"},
            new Object[]{2L, "블루"}));
    when(operations.withRefreshPolicy(any())).thenReturn(operations);

    int total = reindexService.reindexAll();

    assertThat(total).isEqualTo(2);
    verify(operations).save(docsCaptor.capture(), any(IndexCoordinates.class));
    List<ProductDocument> docs = docsCaptor.getValue();
    assertThat(docs).hasSize(2);

    Map<Long, List<String>> optionsById = docs.stream()
        .collect(Collectors.toMap(ProductDocument::getId, ProductDocument::getOptionValues));
    assertThat(optionsById.get(1L)).containsExactlyInAnyOrder("흰색", "검정");
    assertThat(optionsById.get(2L)).containsExactly("블루");
  }

  @Test
  @DisplayName("옵션이 없는 상품은 빈 옵션값 목록으로 색인된다")
  void reindexAll_indexesWithEmptyOptions_whenProductHasNoOptions() {
    Product p1 = product(1L);
    when(productRepo.findIndexableForReindex(any(), any(), any()))
        .thenReturn(List.of(p1))
        .thenReturn(List.of());
    when(productOptionRepo.findOptionNamesByProductIds(List.of(1L)))
        .thenReturn(List.of());
    when(operations.withRefreshPolicy(any())).thenReturn(operations);

    int total = reindexService.reindexAll();

    assertThat(total).isEqualTo(1);
    verify(operations).save(docsCaptor.capture(), any(IndexCoordinates.class));
    assertThat(docsCaptor.getValue()).hasSize(1);
    assertThat(docsCaptor.getValue().get(0).getOptionValues()).isEmpty();
  }

  @Test
  @DisplayName("색인 대상이 없으면 0을 반환하고 저장을 호출하지 않는다")
  void reindexAll_returnsZero_whenNothingToIndex() {
    when(productRepo.findIndexableForReindex(any(), any(), any())).thenReturn(List.of());

    int total = reindexService.reindexAll();

    assertThat(total).isZero();
    verify(operations, never()).save(any(), any(IndexCoordinates.class));
  }

  @Test
  @DisplayName("재색인 중에 취소되면 작업을 중단한다")
  void reindexAll_stops_whenCancelledDuringExecution() {
    Product p1 = product(1L);
    when(productRepo.findIndexableForReindex(any(), any(), any()))
        .thenAnswer(invocation -> {
          reindexService.cancel();
          return List.of(p1);
        });
    when(productOptionRepo.findOptionNamesByProductIds(List.of(1L)))
        .thenReturn(List.of());
    when(operations.withRefreshPolicy(any())).thenReturn(operations);

    int total = reindexService.reindexAll();

    assertThat(total).isEqualTo(1);
    verify(operations).save(any(), any(IndexCoordinates.class));
  }

  // ── reindexAllAsync() ────────────────────────────────────────────────────────

  @Test
  @DisplayName("이미 재색인이 실행 중이면 조기 반환하고 상태가 변경되지 않는다")
  void reindexAllAsync_alreadyRunning_returnsEarly() {
    // given: isRunning을 true로 설정
    AtomicBoolean isRunning = (AtomicBoolean) ReflectionTestUtils.getField(reindexService, "isRunning");
    isRunning.set(true);

    // when: 비동기 메서드 호출 (@Async 없이 동기 실행)
    reindexService.reindexAllAsync();

    // then: productRepo를 전혀 호출하지 않는다
    verify(productRepo, never()).findIndexableForReindex(any(), any(), any());
  }

  @Test
  @DisplayName("재색인 비동기 실행 정상 완료 시 상태가 COMPLETED가 된다")
  void reindexAllAsync_normalCompletion_statusIsCompleted() {
    // given
    when(productRepo.findIndexableForReindex(any(), any(), any())).thenReturn(List.of());

    // when
    reindexService.reindexAllAsync();

    // then
    ProductReindexService.ReindexStatus status = reindexService.getStatus();
    assertThat(status.state()).isEqualTo("COMPLETED");
    assertThat(status.totalIndexed()).isEqualTo(0);
  }

  @Test
  @DisplayName("재색인 비동기 실행 중 예외 발생 시 상태가 FAILED가 된다")
  void reindexAllAsync_exceptionDuringExecution_statusIsFailed() {
    // given: 첫 번째 청크 조회 시 예외 발생
    when(productRepo.findIndexableForReindex(any(), any(), any()))
        .thenThrow(new RuntimeException("DB 연결 오류"));

    // when
    reindexService.reindexAllAsync();

    // then
    ProductReindexService.ReindexStatus status = reindexService.getStatus();
    assertThat(status.state()).isEqualTo("FAILED");
    assertThat(status.errorMessage()).isEqualTo("DB 연결 오류");
  }

  @Test
  @DisplayName("getStatus는 현재 상태를 반환한다")
  void getStatus_returnsCurrentStatus() {
    // given: 초기 상태는 IDLE
    ProductReindexService.ReindexStatus status = reindexService.getStatus();

    // when & then
    assertThat(status.state()).isEqualTo("IDLE");
    assertThat(status.totalIndexed()).isEqualTo(0);
    assertThat(status.errorMessage()).isNull();
  }

  private Product product(Long id) {
    Category category = org.mockito.Mockito.mock(Category.class);
    when(category.getId()).thenReturn(10L);

    Product product = org.mockito.Mockito.mock(Product.class);
    when(product.getId()).thenReturn(id);
    when(product.getName()).thenReturn("티셔츠" + id);
    when(product.getStatus()).thenReturn(SaleStatus.ON_SALE);
    when(product.getCategory()).thenReturn(category);
    when(product.getDiscountedPrice()).thenReturn(10000);
    when(product.getLikeCount()).thenReturn(5);
    when(product.getCreatedAt()).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));
    return product;
  }
}
