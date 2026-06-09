package com.example.WonkaoTalk.domain.search.indexer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.domain.product.event.ProductIndexRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 색인 이벤트 리스너 단위 테스트 — op 별 위임과 예외 격리(ES 장애가 트랜잭션을 깨지 않음)를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ProductIndexEventListenerTest {

  @Mock
  private ProductIndexService indexService;

  @InjectMocks
  private ProductIndexEventListener listener;

  @Test
  @DisplayName("UPSERT 이벤트는 indexById 로 위임한다")
  void handle_upsert_delegatesToIndexById() {
    listener.handle(ProductIndexRequestedEvent.upsert(1L));

    verify(indexService).indexById(1L);
  }

  @Test
  @DisplayName("DELETE 이벤트는 deleteById 로 위임한다")
  void handle_delete_delegatesToDeleteById() {
    listener.handle(ProductIndexRequestedEvent.delete(2L));

    verify(indexService).deleteById(2L);
  }

  @Test
  @DisplayName("색인 중 예외가 나도 밖으로 전파하지 않는다")
  void handle_swallowsException() {
    doThrow(new RuntimeException("ES down")).when(indexService).indexById(3L);

    assertThatCode(() -> listener.handle(ProductIndexRequestedEvent.upsert(3L)))
        .doesNotThrowAnyException();
  }
}
