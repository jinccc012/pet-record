package com.harumi.petrecord.dailyrecord.dto;

import com.harumi.petrecord.dailyrecord.DailyRecord;
import com.harumi.petrecord.dailyrecord.FeedingRecord;
import com.harumi.petrecord.dailyrecord.StoolRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DailyRecordResponse(
        Long id,
        LocalDate recordDate,
        BigDecimal weightKg,
        Integer waterMl,
        String dailyNote,
        List<FeedingResponse> feedings,
        List<StoolResponse> stools,
        Instant createdAt,
        Instant updatedAt
) {
    public record FeedingResponse(Long id, LocalTime feedingTime, Integer foodGram, String conditionText) {
        static FeedingResponse from(FeedingRecord f) {
            return new FeedingResponse(f.getId(), f.getFeedingTime(), f.getFoodGram(), f.getConditionText());
        }
    }

    public record StoolResponse(Long id, LocalTime stoolTime, String conditionText, boolean abnormal) {
        static StoolResponse from(StoolRecord s) {
            return new StoolResponse(s.getId(), s.getStoolTime(), s.getConditionText(), s.isAbnormal());
        }
    }

    public static DailyRecordResponse from(DailyRecord d) {
        return new DailyRecordResponse(
                d.getId(),
                d.getRecordDate(),
                d.getWeightKg(),
                d.getWaterMl(),
                d.getDailyNote(),
                d.getFeedings().stream().map(FeedingResponse::from).toList(),
                d.getStools().stream().map(StoolResponse::from).toList(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
