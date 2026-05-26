package com.harumi.petrecord.file.dto;

public record SignedUploadUrlResponse(
        Long uploadSessionId,
        String uploadUrl,
        String objectKey,
        long expiresInSeconds
) {
}
