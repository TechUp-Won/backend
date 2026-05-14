package com.example.WonkaoTalk.domain.shipping.dto;

import jakarta.validation.constraints.Pattern;

public record ShippingAddressUpdateRequest(
    String recipientName,

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
    String recipientPhone,

    @Pattern(regexp = "^\\d{5}$", message = "우편번호는 5자리 숫자여야 합니다.")
    String zipCode,
    String address1,
    String address2,
    String memo,
    Boolean isDefault
) {

}
