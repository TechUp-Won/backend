package com.example.WonkaoTalk.domain.product.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartDeleteResponse {

  private Long cartId;
}
