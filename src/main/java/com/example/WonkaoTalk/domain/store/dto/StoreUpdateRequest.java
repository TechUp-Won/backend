package com.example.WonkaoTalk.domain.store.dto;

import jakarta.validation.constraints.Pattern;

public record StoreUpdateRequest(
    String name,

    String description,

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식을 입력해주세요.")
    String phone,

    String thumbnail
) {

}
