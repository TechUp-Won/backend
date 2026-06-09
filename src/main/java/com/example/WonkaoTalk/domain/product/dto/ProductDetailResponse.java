package com.example.WonkaoTalk.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductDetailResponse {

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
  private List<VariantInfo> variants;

  @Getter
  @Builder
  public static class StoreInfo {
    private Long storeId;
    private String storeName;
  }

  @Getter
  @Builder
  public static class ImageInfo {
    private String url;
    private Integer sortOrder;
  }

  @Getter
  @Builder
  public static class DetailInfo {
    private String content;
  }

  @Getter
  @Builder
  public static class OptionGroupInfo {
    private Long productOptionGroupId;
    private String name;
    private List<OptionInfo> options;
  }

  @Getter
  @Builder
  public static class OptionInfo {
    private Long productOptionId;
    private String name;
  }

  @Getter
  @Builder
  public static class VariantInfo {
    private Long variantId;
    private String variantName;
    private List<Long> combinationIds;
    private Integer stock;
    private String status;
  }
}
