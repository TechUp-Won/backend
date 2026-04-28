package com.example.WonkaoTalk.domain.product.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductListRequest {

  private Long categoryId;
  private Long storeId;
  private Integer minPrice;
  private Integer maxPrice;
  private String sort = "popular";
  private Long lastProductId;
  private Integer size = 20;
}
