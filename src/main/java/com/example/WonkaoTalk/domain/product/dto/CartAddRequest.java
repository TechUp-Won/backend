package com.example.WonkaoTalk.domain.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CartAddRequest {

  @NotNull private Long productId;
  @NotNull private Long variantId;
  @NotNull @Min(1) private Integer quantity;
}
