package com.example.WonkaoTalk.domain.search.indexer;

import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.search.document.ProductDocument;
import com.example.WonkaoTalk.domain.search.repo.ProductSearchRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 전체 재색인 배치 — 최초 도입 시 기존 상품 일괄 색인 및 드리프트 발생 시 정합성 회복용.
 *
 * <p>대용량을 고려해 (1) id 키셋 페이징으로 청크 단위 조회, (2) 청크별 옵션값을 한 번에 조회(N+1 회피),
 * (3) ES bulk 색인({@code saveAll})으로 처리한다. 문서 ID = product.id 이므로 멱등하게 재실행 가능.
 *
 * <p>긴 단일 트랜잭션/영속성 컨텍스트 누적을 피하려고 클래스 레벨 트랜잭션을 두지 않는다.
 * category 는 fetch join 으로 초기화되고 나머지 색인 필드는 모두 즉시 로딩이라 지연 로딩 문제는 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReindexService {

  private static final List<SaleStatus> INDEXABLE_STATUSES =
      List.of(SaleStatus.ON_SALE, SaleStatus.OUT_OF_STOCK);
  private static final int BATCH_SIZE = 1000;

  private final ProductRepo productRepo;
  private final ProductOptionRepo productOptionRepo;
  private final ProductSearchRepository searchRepository;

  /** 색인 대상 전체를 청크 단위로 다시 색인한다. 총 색인 건수를 반환. */
  public int reindexAll() {
    int total = 0;
    long lastId = 0L;

    while (true) {
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
      searchRepository.saveAll(docs); // ES bulk 색인

      total += docs.size();
      lastId = ids.get(ids.size() - 1);
      log.info("상품 재색인 진행: 누적 {} 건 (마지막 id={})", total, lastId);
    }

    log.info("상품 전체 재색인 완료: {} 건", total);
    return total;
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
}
