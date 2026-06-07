package com.example.WonkaoTalk.domain.search.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.search.dto.SearchRequest;
import com.example.WonkaoTalk.domain.search.dto.SearchResponse;
import com.example.WonkaoTalk.domain.search.service.SearchService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 검색 컨트롤러 단위 테스트 — 서비스 결과를 200 OK + ApiResponse 로 감싸 반환하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

  @Mock
  private SearchService searchService;

  @InjectMocks
  private SearchController searchController;

  @Test
  @DisplayName("검색 결과를 200 OK 와 SUCCESS 응답으로 감싸 반환한다")
  void search_wrapsServiceResultInOkResponse() {
    SearchRequest request = new SearchRequest("셔츠", null, null, null, "popular", null, null, 20);
    SearchResponse serviceResult = new SearchResponse(List.of(), List.of(), false, null, null);
    when(searchService.search(request)).thenReturn(serviceResult);

    ResponseEntity<ApiResponse<SearchResponse>> result = searchController.search(request);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getStatus()).isEqualTo("SUCCESS");
    assertThat(result.getBody().getData()).isSameAs(serviceResult);
  }
}
