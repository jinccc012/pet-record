package com.harumi.petrecord.dailyrecord.dto;

import com.harumi.petrecord.dailyrecord.DailyChartRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record ChartResponse(
        List<LocalDate> labels,
        List<BigDecimal> weightKg,
        List<Integer> waterMl,
        List<Long> foodGram
) {
    public static ChartResponse from(List<DailyChartRow> rows) {
        List<LocalDate> labels = new ArrayList<>(rows.size());
        List<BigDecimal> weightKg = new ArrayList<>(rows.size());
        List<Integer> waterMl = new ArrayList<>(rows.size());
        List<Long> foodGram = new ArrayList<>(rows.size());
        for (DailyChartRow r : rows) {
            labels.add(r.recordDate());
            weightKg.add(r.weightKg());
            waterMl.add(r.waterMl());
            foodGram.add(r.totalFoodGram());
        }
        return new ChartResponse(labels, weightKg, waterMl, foodGram);
    }
}
