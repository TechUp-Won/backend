package com.example.WonkaoTalk.domain.search.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.search.dto.SearchRequest;
import com.example.WonkaoTalk.domain.search.dto.SearchResponse;
import com.example.WonkaoTalk.domain.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "검색", description = "상품과 스토어 통합 검색 API")
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "통합 검색", description = "검색어와 필터 조건으로 상품과 스토어를 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SearchResponse>> search(
        @ModelAttribute SearchRequest request) {
        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(ApiResponse.success("조회가 완료되었습니다", response));
    }
}
