package com.example.WonkaoTalk.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDetailCacheDto {

  private Long productId;
  private String productName;
  private Integer price;
  private Integer discountedPrice;
  private Integer discountRate;
  private String status;
  private Integer likeCount;
  @JsonProperty("isLiked")
  private boolean isLiked;
  private StoreInfo store;
  private List<ImageInfo> images;
  private DetailInfo detail;
  private List<OptionGroupInfo> optionGroups;
  private List<VariantCacheInfo> variants;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class StoreInfo {
    private Long storeId;
    private String storeName;
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ImageInfo {
    private String url;
    private Integer sortOrder;
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DetailInfo {
    private String content;
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OptionGroupInfo {
    private Long productOptionGroupId;
    private String name;
    private List<OptionInfo> options;
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OptionInfo {
    private Long productOptionId;
    private String name;
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class VariantCacheInfo {
    private Long variantId;
    private String variantName;
    private List<Long> combinationIds;
    private String status;
  }
}
