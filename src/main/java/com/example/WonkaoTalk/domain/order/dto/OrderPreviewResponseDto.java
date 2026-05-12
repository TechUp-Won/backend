package com.example.WonkaoTalk.domain.order.dto;

import java.util.List;

public record OrderPreviewResponseDto(
    List<OrderPreviewItemDto> items,
    SummaryDto summary
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
