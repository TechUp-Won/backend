package com.example.WonkaoTalk.domain.product.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartQuantityUpdateResponse {

  private Integer originalTotalAmount;
  private Integer discountTotalAmount;
  private CartItemDetail cartItem;

  @Getter
  @Builder
  public static class CartItemDetail {
    private Long cartItemId;
    private Long id;
    private String name;
    private Long variantId;
    private String variantName;
    private Integer price;
    private Integer discountedPrice;
    private Integer quantity;
    private Integer stock;
    private String status;
    private String updatedAt;
  }
}
