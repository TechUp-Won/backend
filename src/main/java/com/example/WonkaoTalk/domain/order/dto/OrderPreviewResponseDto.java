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
      Integer price,
      Integer discountedPrice,
      Integer quantity,
      Integer itemOriginalAmount,
      Integer itemDiscountAmount,
      Integer itemFinalAmount
  ) {
  }

  public record SummaryDto(
      Integer originalAmount,
      Integer discountAmount,
      Integer finalAmount
  ) {
  }
}
