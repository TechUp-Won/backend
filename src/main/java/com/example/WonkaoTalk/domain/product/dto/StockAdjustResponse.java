package com.example.WonkaoTalk.domain.product.dto;

import com.example.WonkaoTalk.domain.product.enums.StockChangeReason;
import lombok.Builder;

@Builder
public record StockAdjustResponse(
    Long variantId,
    String variantName,
    int stockBefore,
    int changeAmount,
    int stockAfter,
    StockChangeReason reason
) {

}
