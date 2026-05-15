package com.example.WonkaoTalk.domain.seller.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SellerUpdateRequest(
    @Size(max = 50)
    String name,

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$")
    String phone
) {

}
