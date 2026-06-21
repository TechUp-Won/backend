package com.example.WonkaoTalk.domain.auth.dto;

import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MockSocialLoginRequest(
    @NotNull(message = "provider는 필수입니다.") AuthProvider provider,
    @NotBlank(message = "providerId는 필수입니다.") String providerId,
    @NotBlank(message = "email은 필수입니다.") @Email(message = "올바른 이메일 형식이 아닙니다.") String email
) {

}
