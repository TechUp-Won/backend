package com.example.WonkaoTalk.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(
    String oauthRedirectUri
) {

}
