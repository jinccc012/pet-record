package com.harumi.petrecord.dailyrecord.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record StoolRequest(
        @NotNull LocalTime stoolTime,
        @Size(max = 500) String conditionText,
        boolean abnormal
) {
}
