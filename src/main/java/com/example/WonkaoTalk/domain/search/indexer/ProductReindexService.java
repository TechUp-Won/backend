package com.example.WonkaoTalk.domain.search.indexer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.search.document.ProductDocument;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PreDestroy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 전체 재색인 배치 — 최초 도입 시 기존 상품 일괄 색인 및 드리프트 발생 시 정합성 회복용.
 *
 * 대용량을 고려해 (1) id 키셋 페이징으로 청크 단위 조회, (2) 청크별 옵션값을 한 번에 조회(N+1 회피),
 * (3) ES bulk 색인({@code saveAll})으로 처리한다. 문서 ID = product.id 이므로 멱등하게 재실행 가능.
 *
 * 긴 단일 트랜잭션/영속성 컨텍스트 누적을 피하려고 클래스 레벨 트랜잭션을 두지 않는다.
 * category 는 fetch join 으로 초기화되고 나머지 색인 필드는 모두 즉시 로딩이라 지연 로딩 문제는 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReindexService {

  private static final List<SaleStatus> INDEXABLE_STATUSES =
      List.of(SaleStatus.ON_SALE, SaleStatus.OUT_OF_STOCK);
  private static final int BATCH_SIZE = 2000;
  private static final String INDEX_NAME = "products";
  private static final String DEFAULT_REFRESH_INTERVAL = "1s";

  private final ProductRepo productRepo;
  private final ProductOptionRepo productOptionRepo;
  private final ElasticsearchOperations operations;
  private final ElasticsearchClient esClient;

  private final AtomicBoolean isRunning = new AtomicBoolean(false);
  private volatile ReindexStatus status = new ReindexStatus("IDLE", 0, 0L, null);
  private volatile boolean cancelled = false;

  public ReindexStatus getStatus() {
    return status;
  }

  /** 색인 대상 전체를 청크 단위로 다시 색인한다. 총 색인 건수를 반환 (동기 방식). */
  public int reindexAll() {
    cancelled = false;
    int total = runReindexing(null);
    log.info("상품 전체 재색인 완료: {} 건", total);
    return total;
  }

  /** 백그라운드 스레드에서 전체 재색인 작업을 실행한다 (비동기 방식). */
  @Async
  public void reindexAllAsync() {
    if (!isRunning.compareAndSet(false, true)) {
      log.warn("이미 재색인 작업이 진행 중입니다.");
      return;
    }
    status = new ReindexStatus("RUNNING", 0, 0L, null);
    cancelled = false;
    try {
      int total = runReindexing((t, id) -> status = new ReindexStatus("RUNNING", t, id, null));
      if (cancelled) {
        status = new ReindexStatus("STOPPED", total, status.lastId(), "Application shutdown");
        log.info("상품 전체 재색인 작업이 중단되었습니다: {} 건", total);
      } else {
        status = new ReindexStatus("COMPLETED", total, status.lastId(), null);
        log.info("상품 전체 재색인 완료: {} 건", total);
      }
    } catch (Exception e) {
      if (cancelled) {
        log.warn("애플리케이션 종료 중 재색인 중단 (예상된 예외): {}", e.getMessage());
        status = new ReindexStatus("STOPPED", status.totalIndexed(), status.lastId(), "Application shutdown");
      } else {
        log.error("재색인 중 에러 발생", e);
        status = new ReindexStatus("FAILED", status.totalIndexed(), status.lastId(), e.getMessage());
      }
    } finally {
      isRunning.set(false);
    }
  }

  @PreDestroy
  public void cancel() {
    this.cancelled = true;
    log.info("애플리케이션 종료 감지: 재색인 작업을 중단합니다.");
  }

  private int runReindexing(BiConsumer<Integer, Long> progressConsumer) {
    int total = 0;
    long lastId = 0L;

    // 대량 적재 동안 refresh 를 멈춰 세그먼트 양산/머지 부하를 없앤다. 종료 시 반드시 복원.
    pauseRefresh();
    try {
      while (!cancelled && !Thread.currentThread().isInterrupted()) {
        List<Product> batch = productRepo.findIndexableForReindex(
            INDEXABLE_STATUSES, lastId, PageRequest.of(0, BATCH_SIZE));
        if (batch.isEmpty()) {
          break;
        }

        List<Long> ids = batch.stream().map(Product::getId).toList();
        Map<Long, List<String>> optionsByProduct = groupOptionNames(ids);

        List<ProductDocument> docs = batch.stream()
            .map(p -> ProductDocument.from(p, optionsByProduct.getOrDefault(p.getId(), List.of())))
            .toList();
        // bulk 마다 refresh 를 강제하지 않도록 RefreshPolicy.NONE 으로 색인한다.
        operations.withRefreshPolicy(RefreshPolicy.NONE)
            .save(docs, IndexCoordinates.of(INDEX_NAME));

        total += docs.size();
        lastId = ids.get(ids.size() - 1);
        if (progressConsumer != null) {
          progressConsumer.accept(total, lastId);
        }
        log.info("상품 재색인 진행: 누적 {} 건 (마지막 id={})", total, lastId);
      }
    } finally {
      resumeRefresh();
    }
    return total;
  }

  /** 재색인 동안 refresh_interval 을 -1 로 설정해 자동 refresh 를 멈춘다. 실패해도 재색인은 진행. */
  private void pauseRefresh() {
    try {
      esClient.indices().putSettings(p -> p
          .index(INDEX_NAME)
          .settings(s -> s.refreshInterval(t -> t.time("-1"))));
      log.info("재색인 시작: refresh_interval 비활성화(-1)");
    } catch (Exception e) {
      log.warn("refresh_interval 비활성화 실패 — 기본 설정으로 재색인을 진행합니다.", e);
    }
  }

  /** refresh_interval 을 기본값으로 복원하고 1회 수동 refresh 하여 적재 문서를 검색 가능하게 만든다. */
  private void resumeRefresh() {
    try {
      esClient.indices().putSettings(p -> p
          .index(INDEX_NAME)
          .settings(s -> s.refreshInterval(t -> t.time(DEFAULT_REFRESH_INTERVAL))));
      esClient.indices().refresh(r -> r.index(INDEX_NAME));
      log.info("재색인 종료: refresh_interval 복원({}) 및 수동 refresh 완료", DEFAULT_REFRESH_INTERVAL);
    } catch (Exception e) {
      log.error("refresh_interval 복원 실패 — 인덱스가 refresh 비활성 상태로 남을 수 있습니다. 수동 확인 필요.", e);
    }
  }

  private Map<Long, List<String>> groupOptionNames(List<Long> productIds) {
    Map<Long, List<String>> optionsByProduct = new HashMap<>();
    for (Object[] row : productOptionRepo.findOptionNamesByProductIds(productIds)) {
      Long productId = (Long) row[0];
      String optionName = (String) row[1];
      optionsByProduct.computeIfAbsent(productId, k -> new ArrayList<>()).add(optionName);
    }
    return optionsByProduct;
  }

  public record ReindexStatus(String state, int totalIndexed, long lastId, String errorMessage) {}
}
