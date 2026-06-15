package com.example.WonkaoTalk.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductLikeToggleResponse {

  private Long productId;
  @JsonProperty("isLiked")
  private boolean isLiked;
  private Integer likeCount;
}
