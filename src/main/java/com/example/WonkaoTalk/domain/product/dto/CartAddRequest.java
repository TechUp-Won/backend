package com.example.WonkaoTalk.domain.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CartAddRequest {

  private Long productId;
  private Long variantId;
  private Integer quantity;
}
