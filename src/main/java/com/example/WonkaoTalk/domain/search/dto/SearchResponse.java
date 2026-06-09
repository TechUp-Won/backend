package com.example.WonkaoTalk.domain.search.dto;

import java.util.List;

public record SearchResponse(
    List<StoreResult> stores,
    List<ProductResult> products,
    boolean hasNext,
    Long nextCursorId,
    Long nextCursorSortValue
) {

    public record StoreResult(
        Long storeId,
        String storeName,
        String thumbnail,
        String description
    ) {}

    public record ProductResult(
        Long id,
        String name,
        String thumbnail,
        Integer price,
        Integer discountedPrice,
        Integer discountRate,
        Integer likeCount,
        String status,
        StoreInfo store
    ) {}

    public record StoreInfo(
        Long storeId,
        String storeName
    ) {}
}
