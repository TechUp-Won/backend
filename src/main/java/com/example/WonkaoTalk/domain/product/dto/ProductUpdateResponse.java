package com.example.WonkaoTalk.domain.product.dto;

import java.time.LocalDateTime;

public record ProductUpdateResponse(
    Long productId,
    String name,
    Long categoryId,
    String thumbnail,
    Integer price,
    Integer discountRate,
    Integer discountedPrice,
    String status,
    LocalDateTime updatedAt
) {}
