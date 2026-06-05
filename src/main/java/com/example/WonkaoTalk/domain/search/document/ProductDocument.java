package com.example.WonkaoTalk.domain.search.document;

import com.example.WonkaoTalk.domain.product.entity.Product;
import java.time.ZoneOffset;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * 상품 검색용 ElasticSearch 문서.
 *
 * <p>방식 A(구현 계획 §3): 검색·필터·정렬에 필요한 최소 필드만 색인하고, 표시용 상세 데이터는 DB에서 fetch 한다.
 * {@code name} 과 {@code optionValues} 는 {@code searchText} 로 copy_to 되어, 단일 필드 매칭만으로
 * "흰색 티셔츠"(옵션값 + 상품명) 같은 멀티 토큰 질의를 처리한다.
 */
@Document(indexName = "products", createIndex = false)
@Setting(settingPath = "elasticsearch/product-settings.json")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ProductDocument {

  @Id
  private Long id;

  @Field(type = FieldType.Text, analyzer = "nori_analyzer", copyTo = "searchText")
  private String name;

  @Field(type = FieldType.Text, analyzer = "nori_analyzer", copyTo = "searchText")
  private List<String> optionValues;

  @Field(type = FieldType.Text, analyzer = "nori_analyzer")
  private String searchText;

  @Field(type = FieldType.Long)
  private Long categoryId;

  @Field(type = FieldType.Integer)
  private Integer discountedPrice;

  @Field(type = FieldType.Integer)
  private Integer likeCount;

  @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
  private Long createdAt;

  @Field(type = FieldType.Keyword)
  private String status;

  public static ProductDocument from(Product product, List<String> optionValues) {
    return ProductDocument.builder()
        .id(product.getId())
        .name(product.getName())
        .optionValues(optionValues)
        .categoryId(product.getCategory().getId())
        .discountedPrice(product.getDiscountedPrice())
        .likeCount(product.getLikeCount())
        .createdAt(product.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli())
        .status(product.getStatus().name())
        .build();
  }
}
