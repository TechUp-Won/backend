package com.example.WonkaoTalk.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime updatedAt;
  }

  @Getter
  @Builder
  public static class Summary {
    private Integer originalTotalAmount;
    private Integer discountTotalAmount;
  }
}
