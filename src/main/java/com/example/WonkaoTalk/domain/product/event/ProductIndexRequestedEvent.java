package com.example.WonkaoTalk.domain.product.event;

/**
 * 상품 검색 색인 갱신 요청 이벤트. 색인 필드(상품명/옵션값/상태/가격/카테고리)를 바꾸는 변경 후 발행한다.
 */
public record ProductIndexRequestedEvent(Long productId, IndexOp op) {

  public static ProductIndexRequestedEvent upsert(Long productId) {
    return new ProductIndexRequestedEvent(productId, IndexOp.UPSERT);
  }

  public static ProductIndexRequestedEvent delete(Long productId) {
    return new ProductIndexRequestedEvent(productId, IndexOp.DELETE);
  }

  public enum IndexOp {
    UPSERT, DELETE
  }
}
