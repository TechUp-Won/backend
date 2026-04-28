package com.example.WonkaoTalk.domain.product.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductListResponse {

  private List<ProductSummary> products;
  private boolean hasNext;
  private Long nextCursorId;
  private Long nextCursorSortValue;

  @Getter
  @Builder
  public static class ProductSummary {
    private Long id;
    private String name;
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
