package com.example.WonkaoTalk.domain.product.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CartOptionUpdateRequest {

  @NotNull private Long variantId;
}
