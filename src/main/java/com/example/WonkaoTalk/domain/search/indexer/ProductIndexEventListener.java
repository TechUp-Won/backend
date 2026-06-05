package com.example.WonkaoTalk.domain.search.indexer;

import com.example.WonkaoTalk.domain.product.event.ProductIndexRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 상품 변경 트랜잭션 커밋(AFTER_COMMIT) 후 ES 색인을 갱신한다.
 * ES 장애가 비즈니스 트랜잭션을 롤백시키지 않도록 예외를 격리하고, 드리프트는 재색인 배치로 회복한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexEventListener {

  private final ProductIndexService indexService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ProductIndexRequestedEvent event) {
    try {
      switch (event.op()) {
        case UPSERT -> indexService.indexById(event.productId());
        case DELETE -> indexService.deleteById(event.productId());
      }
    } catch (Exception e) {
      log.error("상품 색인 동기화 실패 productId={}, op={}", event.productId(), event.op(), e);
    }
  }
}
