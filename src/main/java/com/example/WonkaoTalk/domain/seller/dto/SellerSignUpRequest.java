package com.example.WonkaoTalk.domain.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record SellerSignUpRequest(
    @NotBlank(message = "이메일을 입력해주세요.")
    @Pattern(
        regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$",
        message = "올바른 이메일 형식이 아닙니다."
    ) String email,

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$",
        message = "비밀번호는 영문 대/소문자와 숫자를 포함하여 8자리 이상이어야 합니다."
    ) String password,

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    String passwordCheck,

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
