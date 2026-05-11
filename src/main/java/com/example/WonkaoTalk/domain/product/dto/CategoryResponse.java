package com.example.WonkaoTalk.domain.product.dto;

import java.util.List;

public record CategoryResponse(
    Long id,
    String name,
    Integer depth,
    List<CategoryResponse> children
) {

}
