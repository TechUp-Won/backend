package com.example.WonkaoTalk.common.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(
    @NotBlank
    String oauthRedirectUri
) {

}
