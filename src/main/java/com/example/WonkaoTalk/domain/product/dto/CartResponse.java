package com.example.WonkaoTalk.domain.product.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartResponse {

  private Long cartId;
  private List<CartItemInfo> cartItems;
  private Summary summary;

  @Getter
  @Builder
  public static class CartItemInfo {
    private Long cartItemId;
    private Long id;
    private String name;
    private Integer price;
    private Integer discountedPrice;
    private Integer discountRate;
    private String thumbnail;
    private Long variantId;
    private String variantName;
    private Integer quantity;
    private Integer stock;
    private String status;
    private String updatedAt;
  }

  @Getter
  @Builder
  public static class Summary {
    private Integer originalTotalAmount;
    private Integer discountTotalAmount;
  }
}
