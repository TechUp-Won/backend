package com.example.WonkaoTalk.domain.auth.event;

import com.example.WonkaoTalk.domain.auth.entity.Auth;

public record OAuth2UserCreatedEvent(
    Auth auth,
    String email,
    String name
) {

}
