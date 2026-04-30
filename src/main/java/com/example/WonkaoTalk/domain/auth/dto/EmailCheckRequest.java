package com.example.WonkaoTalk.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailCheckRequest(
    @NotBlank(message = "이메일을 입력해주세요.")
    @Pattern(
        // OWASP 이메일 정규식
        regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$",
        message = "올바른 이메일 형식이 아닙니다."
    )
    String email
) {

}
