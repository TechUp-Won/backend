package com.example.WonkaoTalk.domain.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StoreCreateRequest(
    @NotBlank(message = "스토어 이름은 필수 입력값입니다.")
    String name,

    String description,

    @NotBlank(message = "전화번호는 필수 입력값입니다.")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식을 입력해주세요.")
    String phone,

    String thumbnail
) {

}
