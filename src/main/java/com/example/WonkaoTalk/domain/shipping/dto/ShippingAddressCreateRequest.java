package com.example.WonkaoTalk.domain.shipping.dto;

import jakarta.validation.constraints.NotBlank;

public record ShippingAddressCreateRequest(
    @NotBlank(message = "수령인 이름은 필수입니다.")
    String recipientName,

    @NotBlank(message = "수령인 연락처는 필수입니다.")
    String recipientPhone,

    @NotBlank(message = "우편번호는 필수입니다.")
    String zipCode,

    @NotBlank(message = "기본 주소는 필수입니다.")
    String address1,

    String address2,

    String memo
) {

}
