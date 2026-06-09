package com.example.WonkaoTalk.domain.search.repo;

import java.util.List;

/**
 * ES 상품 검색 결과 — 매칭/정렬된 상품 ID 목록과 커서 정보.
 * 상세 데이터는 이 ID 목록으로 DB 에서 fetch 한다(구현 계획 §3 방식 A).
 */
public record ProductSearchResult(
    List<Long> ids,
    boolean hasNext,
    Long nextCursorId,
    Long nextCursorSortValue
) {

  public static ProductSearchResult empty() {
    return new ProductSearchResult(List.of(), false, null, null);
  }
}
