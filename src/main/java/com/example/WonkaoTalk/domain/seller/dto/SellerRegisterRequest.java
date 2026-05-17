package com.example.WonkaoTalk.domain.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record SellerRegisterRequest(
    @NotBlank(message = "사업자 등록번호는 필수입니다.")
    @Pattern(
        regexp = "^\\d{10}$",
        message = "사업자 번호는 숫자 10자리여야 합니다."
    ) String buzNo,

    @Size(max = 50)
    @NotBlank(message = "사업장명은 필수입니다.")
    String name,

    @NotBlank(message = "사업장 연락처는 필수입니다.")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$")
    String phone
) {

}
