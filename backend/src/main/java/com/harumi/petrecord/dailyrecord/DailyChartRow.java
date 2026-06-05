package com.harumi.petrecord.dailyrecord;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyChartRow(
        LocalDate recordDate,
        BigDecimal weightKg,
        Integer waterMl,
        Long totalFoodGram
) {
}
