package com.example.WonkaoTalk.domain.order.dto;

import java.util.List;

public record OrderPreviewResponse(
    List<OrderPreviewItemDto> items,
    SummaryDto summary
    // TODO: 사용 가능한 포인트 계산해서 같이 보내주기
) {

  public record OrderPreviewItemDto(
      Long productId,
      Long variantId,
      String productName,
      String variantName,
      String thumbnailUrl,
      Long price,
      Long discountedPrice,
      Integer quantity,
      Long itemOriginalAmount,
      Long itemDiscountAmount,
      Long itemFinalAmount
  ) {

  }

  public record SummaryDto(
      Long originalAmount,
      Long discountAmount,
      Long finalAmount
  ) {

  }
}
