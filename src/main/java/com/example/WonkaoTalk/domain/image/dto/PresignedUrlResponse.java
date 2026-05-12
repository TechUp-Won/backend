package com.example.WonkaoTalk.domain.image.dto;

public record PresignedUrlResponse(
    String uploadUrl,
    String objectKey,
    String contentType
) {}
