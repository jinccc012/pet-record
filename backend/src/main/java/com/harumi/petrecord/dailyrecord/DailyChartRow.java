package com.harumi.petrecord.dailyrecord;

import java.math.BigDecimal;
import java.time.LocalDate;

// JPQL constructor-expression projection for the chart query.
public record DailyChartRow(
        LocalDate recordDate,
        BigDecimal weightKg,
        Integer waterMl,
        Long totalFoodGram
) {
}
