package com.harumi.petrecord.file.dto;

import jakarta.validation.constraints.NotNull;

public record CompleteUploadRequest(
        @NotNull Long uploadSessionId
) {
}
