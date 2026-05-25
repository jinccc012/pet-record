package com.harumi.petrecord.dailyrecord.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateDailyRecordRequest(
        @NotNull @PastOrPresent LocalDate recordDate,
        @DecimalMin("0.0") @DecimalMax("999.99") @Digits(integer = 3, fraction = 2) BigDecimal weightKg,
        @PositiveOrZero Integer waterMl,
        @Size(max = 1000) String dailyNote,
        @Valid List<FeedingRequest> feedings,
        @Valid List<StoolRequest> stools
) {
}
