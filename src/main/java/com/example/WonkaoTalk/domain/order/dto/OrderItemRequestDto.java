package com.example.WonkaoTalk.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequestDto(
    @NotNull
    Long variantId,

    @NotNull
    @Min(1)
    Integer quantity
) {
}
