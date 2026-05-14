package com.example.WonkaoTalk.domain.user.dto;

import com.example.WonkaoTalk.domain.user.enums.Gender;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UserUpdateRequest(
    @Size(max = 20)
    String nickname,
    String image,
    Gender gender,
    LocalDate birthDate,
    Boolean marketingAgree
) {

}
