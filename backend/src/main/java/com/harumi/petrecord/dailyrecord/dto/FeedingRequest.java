package com.harumi.petrecord.dailyrecord.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record FeedingRequest(
        @NotNull LocalTime feedingTime,
        @PositiveOrZero Integer foodGram,
        @Size(max = 500) String conditionText
) {
}
