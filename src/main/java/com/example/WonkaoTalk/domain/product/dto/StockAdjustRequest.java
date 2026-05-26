package com.example.WonkaoTalk.domain.product.dto;

import com.example.WonkaoTalk.domain.product.enums.StockChangeReason;
import jakarta.validation.constraints.NotNull;

public record StockAdjustRequest(

    @NotNull
    Integer changeAmount,

    @NotNull
    StockChangeReason reason
) {

}
