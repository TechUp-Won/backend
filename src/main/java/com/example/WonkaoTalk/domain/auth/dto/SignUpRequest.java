package com.example.WonkaoTalk.domain.auth.dto;

import com.example.WonkaoTalk.domain.user.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record SignUpRequest(
    @NotBlank(message = "이메일을 입력해주세요.")
    @Pattern(
        regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$",
        message = "올바른 이메일 형식이 아닙니다."
    )
    String email,

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$",
        message = "비밀번호는 영문 대/소문자와 숫자를 포함하여 8자리 이상이어야 합니다."
    )
    String password,

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    String passwordCheck,

    @NotBlank(message = "이름을 입력해주세요.")
    String name,

    @NotBlank(message = "닉네임을 입력해주세요.")
    String nickname,

    @NotBlank(message = "전화번호를 입력해주세요.")
    @Pattern(
        regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
        message = "올바른 전화번호 형식(000-0000-0000)이 아닙니다."
    )
    String phone,

    LocalDate birthDate,

    @NotNull(message = "성별을 선택해주세요.")
    Gender gender
) {

}
