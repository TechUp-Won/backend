package com.example.WonkaoTalk.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record LoginRequest(
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Pattern(
        // OWASP 이메일 정규식
        regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$",
        message = "올바른 이메일 형식이 아닙니다."
    )
    String email,
    
    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    String password
) {

}
