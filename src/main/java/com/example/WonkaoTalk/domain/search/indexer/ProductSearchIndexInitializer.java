package com.example.WonkaoTalk.domain.search.indexer;

import com.example.WonkaoTalk.domain.search.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * 부팅 시 products 인덱스가 없으면 settings(nori)/mapping 과 함께 생성한다.
 *
 * {@code @Document(createIndex = false)} 로 SDE 의 즉시 생성을 끄고 이 컴포넌트가 명시적으로 생성한다.
 * ES 연결 실패 시에도 애플리케이션 부팅을 막지 않도록 예외를 격리한다.
 * 테스트 등 ES 없는 환경은 {@code search.index.auto-create=false} 로 비활성화한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "search.index.auto-create", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ProductSearchIndexInitializer implements ApplicationRunner {

  private final ElasticsearchOperations operations;

  @Override
  public void run(ApplicationArguments args) {
    try {
      IndexOperations indexOps = operations.indexOps(ProductDocument.class);
      if (!indexOps.exists()) {
        indexOps.createWithMapping();
        log.info("ES products 인덱스를 생성했습니다.");
      }
    } catch (Exception e) {
      log.warn("ES products 인덱스 초기화 실패 — 검색 사용 전 ES 연결 및 재색인을 확인하세요.", e);
    }
  }
}
