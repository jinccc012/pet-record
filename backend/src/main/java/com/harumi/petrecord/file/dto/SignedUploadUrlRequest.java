package com.harumi.petrecord.file.dto;

import com.harumi.petrecord.file.FileCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SignedUploadUrlRequest(
        @NotNull Long petId,
        @NotNull FileCategory category,
        @NotBlank @Size(max = 255) String originalFilename,
        @NotBlank String contentType,
        @NotNull @Positive Long fileSize
) {
}
