package com.harumi.petrecord.file.dto;

public record SignedViewUrlResponse(
        String url,
        long expiresInSeconds
) {
}
