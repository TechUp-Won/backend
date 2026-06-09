package com.example.WonkaoTalk.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime updatedAt;
  }
}
