package com.example.WonkaoTalk.domain.search.indexer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.domain.search.document.ProductDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

/**
 * 부팅 시 인덱스 초기화 단위 테스트 — 부재 시 생성, 존재 시 스킵, ES 장애 시 부팅 비차단을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductSearchIndexInitializerTest {

  @Mock
  private ElasticsearchOperations operations;

  @Mock
  private IndexOperations indexOps;

  @InjectMocks
  private ProductSearchIndexInitializer initializer;

  @Test
  @DisplayName("인덱스가 없으면 매핑과 함께 생성한다")
  void run_createsIndex_whenAbsent() {
    when(operations.indexOps(ProductDocument.class)).thenReturn(indexOps);
    when(indexOps.exists()).thenReturn(false);

    initializer.run(null);

    verify(indexOps).createWithMapping();
  }

  @Test
  @DisplayName("인덱스가 이미 있으면 생성하지 않는다")
  void run_skipsCreate_whenExists() {
    when(operations.indexOps(ProductDocument.class)).thenReturn(indexOps);
    when(indexOps.exists()).thenReturn(true);

    initializer.run(null);

    verify(indexOps, never()).createWithMapping();
  }

  @Test
  @DisplayName("ES 연결 실패 시 예외를 격리하여 부팅을 막지 않는다")
  void run_swallowsException_whenElasticsearchUnavailable() {
    when(operations.indexOps(ProductDocument.class))
        .thenThrow(new RuntimeException("ES unavailable"));

    assertThatCode(() -> initializer.run(null)).doesNotThrowAnyException();
  }
}
