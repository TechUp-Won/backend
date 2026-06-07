package com.example.WonkaoTalk.domain.search.repo;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.example.WonkaoTalk.domain.product.enums.ProductSortType;
import com.example.WonkaoTalk.domain.search.document.ProductDocument;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductSearchQueryRepositoryImpl implements ProductSearchQueryRepository {

  private static final List<FieldValue> VISIBLE_STATUSES =
      List.of(FieldValue.of("ON_SALE"), FieldValue.of("OUT_OF_STOCK"));

  private final ElasticsearchOperations operations;

  @Override
  public ProductSearchResult search(
      String keyword,
      List<Long> categoryIds,
      Integer minPrice,
      Integer maxPrice,
      ProductSortType sortType,
      Long lastId,
      Long lastSortValue,
      int size
  ) {
    // 상품명 + 옵션값(searchText copy_to)에 모든 토큰이 존재해야 매칭(AND).
    // nori(형태소 통째 토큰)로 못 잡는 부분 문자열("셔츠"→"티셔츠")은 ngram 서브필드로 보완한다.
    // 두 방식 중 하나라도 매칭되면(should/min 1) 결과에 포함.
    Query noriMatch = Query.of(q -> q.match(m -> m
        .field("searchText")
        .query(keyword)
        .operator(Operator.And)));
    Query ngramMatch = Query.of(q -> q.match(m -> m
        .field("searchText.ngram")
        .query(keyword)
        .operator(Operator.And)));
    Query match = Query.of(q -> q.bool(b -> b
        .should(noriMatch)
        .should(ngramMatch)
        .minimumShouldMatch("1")));

    List<Query> filters = buildFilters(categoryIds, minPrice, maxPrice);

    Query query = Query.of(q -> q.bool(b -> b.must(match).filter(filters)));

    Sort sort = Sort.by(primaryDirection(sortType), sortField(sortType))
        .and(Sort.by(Sort.Direction.DESC, "id"));

    NativeQueryBuilder builder = NativeQuery.builder()
        .withQuery(query)
        .withPageable(PageRequest.of(0, size + 1, sort));

    if (lastId != null && lastSortValue != null) {
      builder.withSearchAfter(List.of(lastSortValue, lastId));
    }

    SearchHits<ProductDocument> hits = operations.search(builder.build(), ProductDocument.class);

    List<SearchHit<ProductDocument>> hitList = hits.getSearchHits();
    boolean hasNext = hitList.size() > size;
    if (hasNext) {
      hitList = hitList.subList(0, size);
    }

    List<Long> ids = hitList.stream()
        .map(h -> h.getContent().getId())
        .toList();

    Long nextCursorId = null;
    Long nextCursorSortValue = null;
    if (hasNext && !hitList.isEmpty()) {
      List<Object> sortValues = hitList.get(hitList.size() - 1).getSortValues();
      nextCursorSortValue = ((Number) sortValues.get(0)).longValue();
      nextCursorId = ((Number) sortValues.get(1)).longValue();
    }

    return new ProductSearchResult(ids, hasNext, nextCursorId, nextCursorSortValue);
  }

  private List<Query> buildFilters(List<Long> categoryIds, Integer minPrice, Integer maxPrice) {
    List<Query> filters = new ArrayList<>();

    filters.add(Query.of(q -> q.terms(t -> t
        .field("status")
        .terms(tt -> tt.value(VISIBLE_STATUSES)))));

    if (categoryIds != null && !categoryIds.isEmpty()) {
      List<FieldValue> values = categoryIds.stream()
          .map(id -> FieldValue.of(id.longValue()))
          .toList();
      filters.add(Query.of(q -> q.terms(t -> t
          .field("categoryId")
          .terms(tt -> tt.value(values)))));
    }

    if (minPrice != null || maxPrice != null) {
      filters.add(Query.of(q -> q.range(r -> r.untyped(u -> {
        u.field("discountedPrice");
        if (minPrice != null) {
          u.gte(JsonData.of(minPrice));
        }
        if (maxPrice != null) {
          u.lte(JsonData.of(maxPrice));
        }
        return u;
      }))));
    }

    return filters;
  }

  private String sortField(ProductSortType sortType) {
    return switch (sortType) {
      case POPULAR -> "likeCount";
      case LATEST -> "createdAt";
      case PRICE_ASC, PRICE_DESC -> "discountedPrice";
    };
  }

  private Sort.Direction primaryDirection(ProductSortType sortType) {
    return switch (sortType) {
      case POPULAR, LATEST, PRICE_DESC -> Sort.Direction.DESC;
      case PRICE_ASC -> Sort.Direction.ASC;
    };
  }
}
