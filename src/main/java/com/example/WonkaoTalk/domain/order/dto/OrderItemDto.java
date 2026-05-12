package com.example.WonkaoTalk.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemDto(
    @NotNull
    Long variantId,

    @NotNull
    @Min(1)
    Integer quantity
) {
}
