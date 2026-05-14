package com.example.WonkaoTalk.domain.user.dto;

import com.example.WonkaoTalk.domain.user.enums.Gender;
import java.time.LocalDate;

public record UserUpdateRequest(
    String nickname,
    String image,
    Gender gender,
    LocalDate birthDate,
    boolean marketingAgree
) {

}
