package com.example.WonkaoTalk.domain.product.dto;

import com.example.WonkaoTalk.domain.order.dto.PageInfoDto;
import java.time.LocalDateTime;
import java.util.List;

public record ProductLikeListResponse(
    List<LikeSummary> likes,
    PageInfoDto pageInfo
) {

  public record LikeSummary(
      Long productId,
      String name,
      String thumbnail,
      Integer price,
      Integer discountRate,
      Integer discountedPrice,
      Integer likeCount,
      LocalDateTime likedAt
  ) {

  }
}
