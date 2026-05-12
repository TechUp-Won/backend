package com.example.WonkaoTalk.domain.image.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignedUrlRequest(
    @NotBlank String filename
) {}
