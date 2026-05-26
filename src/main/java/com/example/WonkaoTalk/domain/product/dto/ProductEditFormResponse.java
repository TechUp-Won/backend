package com.example.WonkaoTalk.domain.product.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductEditFormResponse {

  private Long productId;
  private String name;
  private Long categoryId;
  private String thumbnail;
  private Integer price;
  private Integer discountRate;
  private Integer discountedPrice;
  private String status;
  private String detail;
  private List<ImageInfo> images;
  private List<OptionGroupInfo> optionGroups;
  private List<VariantInfo> variants;

  @Getter
  @Builder
  public static class ImageInfo {
    private Long imageId;
    private String url;
    private Integer sortOrder;
  }

  @Getter
  @Builder
  public static class OptionGroupInfo {
    private Long optionGroupId;
    private String name;
    private Integer sortOrder;
    private List<OptionInfo> options;
  }

  @Getter
  @Builder
  public static class OptionInfo {
    private Long optionId;
    private String name;
  }

  @Getter
  @Builder
  public static class VariantInfo {
    private Long variantId;
    private String variantName;
    private Integer stock;
    private String status;
  }
}
