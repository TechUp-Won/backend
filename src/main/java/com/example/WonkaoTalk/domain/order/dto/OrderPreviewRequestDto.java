package com.example.WonkaoTalk.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

// 주문 전 주문 요청서 생성용 API
public record OrderPreviewRequestDto(
    @Valid
    @NotEmpty
    List<OrderItemRequestDto> items
) {
}
