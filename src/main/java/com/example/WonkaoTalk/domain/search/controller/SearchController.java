package com.example.WonkaoTalk.domain.search.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.search.dto.SearchRequest;
import com.example.WonkaoTalk.domain.search.dto.SearchResponse;
import com.example.WonkaoTalk.domain.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SearchResponse>> search(
        @ModelAttribute SearchRequest request) {
        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(ApiResponse.success("조회가 완료되었습니다", response));
    }
}
