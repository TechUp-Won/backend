package com.example.WonkaoTalk.domain.search.repo;

import com.example.WonkaoTalk.domain.search.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 상품 문서 색인/삭제용 Spring Data Elasticsearch 레포지토리.
 * 검색(매칭/정렬/페이지네이션)은 {@link ProductSearchQueryRepository} 에서 처리한다.
 */
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {

}
