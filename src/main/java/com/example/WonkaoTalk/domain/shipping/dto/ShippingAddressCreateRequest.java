package com.example.WonkaoTalk.domain.shipping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ShippingAddressCreateRequest(
    @NotBlank(message = "수령인 이름은 필수입니다.")
    String recipientName,

    @NotBlank(message = "수령인 연락처는 필수입니다.")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
    String recipientPhone,

    @NotBlank(message = "우편번호는 필수입니다.")
    @Pattern(regexp = "^\\d{5}$", message = "우편번호는 5자리 숫자여야 합니다.")
    String zipCode,

    @NotBlank(message = "기본 주소는 필수입니다.")
    String address1,

    String address2,

    String memo
) {

}
