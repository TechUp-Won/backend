package com.example.WonkaoTalk.domain.search.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.search.dto.SearchRequest;
import com.example.WonkaoTalk.domain.search.dto.SearchResponse;
import com.example.WonkaoTalk.domain.search.indexer.ProductReindexService;
import com.example.WonkaoTalk.domain.search.indexer.ProductReindexService.ReindexStatus;
import com.example.WonkaoTalk.domain.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "검색", description = "상품과 스토어 통합 검색 API")
public class SearchController {

    private final SearchService searchService;
    private final ProductReindexService reindexService;

    @Operation(summary = "통합 검색", description = "검색어와 필터 조건으로 상품과 스토어를 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SearchResponse>> search(
        @ModelAttribute SearchRequest request) {
        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(ApiResponse.success("조회가 완료되었습니다", response));
    }

    @Operation(summary = "전체 재색인 (비동기)", description = "데이터베이스의 상품 데이터를 전체 재색인합니다.")
    @PostMapping("/search/reindex")
    public ResponseEntity<ApiResponse<ReindexStatus>> reindex() {
        reindexService.reindexAllAsync();
        return ResponseEntity.accepted()
            .body(ApiResponse.success("재색인 작업이 시작되었습니다.", reindexService.getStatus()));
    }

    @Operation(summary = "재색인 작업 상태 조회", description = "현재 진행 중인 재색인 작업 상태를 조회합니다.")
    @GetMapping("/search/reindex/status")
    public ResponseEntity<ApiResponse<ReindexStatus>> getReindexStatus() {
        return ResponseEntity.ok(
            ApiResponse.success("재색인 작업 상태 조회 성공", reindexService.getStatus()));
    }
}
