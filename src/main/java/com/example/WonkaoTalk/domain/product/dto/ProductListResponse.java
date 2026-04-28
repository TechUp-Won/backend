package com.example.WonkaoTalk.domain.product.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductListResponse {

  private List<ProductSummary> products;
  private boolean hasNext;

  @Getter
  @Builder
  public static class ProductSummary {
    private Long productId;
    private String productName;
    private String thumbnail;
    private Integer price;
    private Integer discountedPrice;
    private Integer discountRate;
    private Integer likeCount;
    private String status;
    private StoreInfo store;
  }

  @Getter
  @Builder
  public static class StoreInfo {
    private Long storeId;
    private String storeName;
  }
}
