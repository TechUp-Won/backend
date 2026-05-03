package com.example.WonkaoTalk.domain.search.dto;

public record SearchRequest(
    String keyword,
    Long categoryId,
    Integer minPrice,
    Integer maxPrice,
    String sort,
    Long lastId,
    Long lastSortValue,
    Integer size
) {}
