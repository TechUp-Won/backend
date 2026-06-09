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
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * 상품 검색용 ElasticSearch 문서.
 *
 * 방식 A(구현 계획 §3): 검색·필터·정렬에 필요한 최소 필드만 색인하고, 표시용 상세 데이터는 DB에서 fetch 한다.
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

  /**
   * {@code name} + {@code optionValues} 가 copy_to 되는 통합 검색 필드.
   *
   * 본필드는 nori(형태소 단위 통째 토큰 매칭)로, {@code searchText.ngram} 서브필드는 ngram(부분 문자열)으로
   * 색인한다. nori 만으로는 "티셔츠"가 단일 토큰이라 "셔츠" 부분 검색이 매칭되지 않으므로 ngram 으로 보완한다.
   */
  @MultiField(
      mainField = @Field(type = FieldType.Text, analyzer = "nori_analyzer"),
      otherFields = @InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "ngram_analyzer"))
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
