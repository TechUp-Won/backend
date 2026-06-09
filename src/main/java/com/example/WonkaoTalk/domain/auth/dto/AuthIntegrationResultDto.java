package com.example.WonkaoTalk.domain.auth.dto;

import com.example.WonkaoTalk.domain.auth.entity.Auth;


public record AuthIntegrationResultDto(
    Auth auth,
    boolean isNewCreated
) {

}
