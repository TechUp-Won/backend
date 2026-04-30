package com.example.WonkaoTalk.domain.auth.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record SignUpResponse(
    Long authId,
    Long userId,
    LocalDateTime createdAt
) {

}
