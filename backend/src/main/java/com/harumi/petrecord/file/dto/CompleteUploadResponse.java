package com.harumi.petrecord.file.dto;

import com.harumi.petrecord.file.FileCategory;

public record CompleteUploadResponse(
        Long fileId,
        FileCategory category,
        String originalFilename,
        String contentType,
        Long fileSize
) {
}
