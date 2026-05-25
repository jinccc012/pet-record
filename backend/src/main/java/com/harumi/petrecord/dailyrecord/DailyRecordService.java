package com.harumi.petrecord.dailyrecord;

import com.harumi.petrecord.common.exception.DuplicateResourceException;
import com.harumi.petrecord.common.exception.ResourceNotFoundException;
import com.harumi.petrecord.dailyrecord.dto.ChartResponse;
import com.harumi.petrecord.dailyrecord.dto.CreateDailyRecordRequest;
import com.harumi.petrecord.dailyrecord.dto.DailyRecordResponse;
import com.harumi.petrecord.dailyrecord.dto.FeedingRequest;
import com.harumi.petrecord.dailyrecord.dto.StoolRequest;
import com.harumi.petrecord.dailyrecord.dto.UpdateDailyRecordRequest;
import com.harumi.petrecord.pet.PetRepository;
import com.harumi.petrecord.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class DailyRecordService {

    private static final Logger log = LoggerFactory.getLogger(DailyRecordService.class);

    private final DailyRecordRepository dailyRecordRepository;
    private final PetRepository petRepository;

    public DailyRecordService(DailyRecordRepository dailyRecordRepository, PetRepository petRepository) {
        this.dailyRecordRepository = dailyRecordRepository;
        this.petRepository = petRepository;
    }

    @Transactional
    public DailyRecordResponse create(CurrentUser currentUser, Long petId, CreateDailyRecordRequest request) {
        assertPetOwned(currentUser, petId);
        if (dailyRecordRepository.existsByPetIdAndRecordDate(petId, request.recordDate())) {
            throw new DuplicateResourceException("A record for this date already exists");
        }
        DailyRecord record = DailyRecord.builder()
                .petId(petId)
                .recordDate(request.recordDate())
                .weightKg(request.weightKg())
                .waterMl(request.waterMl())
                .dailyNote(request.dailyNote())
                .build();
        record.replaceFeedings(toFeedings(request.feedings()));
        record.replaceStools(toStools(request.stools()));
        DailyRecord saved = dailyRecordRepository.save(record);
        log.info("Created daily record id={} petId={}", saved.getId(), petId);
        return DailyRecordResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DailyRecordResponse> list(CurrentUser currentUser, Long petId, LocalDate date) {
        assertPetOwned(currentUser, petId);
        if (date != null) {
            return dailyRecordRepository.findByPetIdAndRecordDate(petId, date)
                    .map(DailyRecordResponse::from)
                    .map(List::of)
                    .orElseGet(List::of);
        }
        return dailyRecordRepository.findAllByPetIdOrderByRecordDateDesc(petId).stream()
                .map(DailyRecordResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DailyRecordResponse get(CurrentUser currentUser, Long petId, Long recordId) {
        assertPetOwned(currentUser, petId);
        return DailyRecordResponse.from(loadOwned(petId, recordId));
    }

    @Transactional
    public DailyRecordResponse update(CurrentUser currentUser, Long petId, Long recordId,
                                      UpdateDailyRecordRequest request) {
        assertPetOwned(currentUser, petId);
        DailyRecord record = loadOwned(petId, recordId);
        record.setWeightKg(request.weightKg());
        record.setWaterMl(request.waterMl());
        record.setDailyNote(request.dailyNote());
        record.replaceFeedings(toFeedings(request.feedings()));
        record.replaceStools(toStools(request.stools()));
        log.info("Updated daily record id={} petId={}", recordId, petId);
        return DailyRecordResponse.from(record);
    }

    @Transactional
    public void delete(CurrentUser currentUser, Long petId, Long recordId) {
        assertPetOwned(currentUser, petId);
        DailyRecord record = loadOwned(petId, recordId);
        dailyRecordRepository.delete(record);
        log.info("Soft-deleted daily record id={} petId={}", recordId, petId);
    }

    @Transactional(readOnly = true)
    public ChartResponse chart(CurrentUser currentUser, Long petId, LocalDate from, LocalDate to) {
        assertPetOwned(currentUser, petId);
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            throw new IllegalArgumentException("range must not exceed 366 days");
        }
        return ChartResponse.from(dailyRecordRepository.findChartRows(petId, from, to));
    }

    private void assertPetOwned(CurrentUser currentUser, Long petId) {
        petRepository.findByIdAndOwnerId(petId, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
    }

    private DailyRecord loadOwned(Long petId, Long recordId) {
        return dailyRecordRepository.findByIdAndPetId(recordId, petId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily record not found"));
    }

    private List<FeedingRecord> toFeedings(List<FeedingRequest> requests) {
        return Optional.ofNullable(requests).orElseGet(List::of).stream()
                .map(f -> FeedingRecord.builder()
                        .feedingTime(f.feedingTime())
                        .foodGram(f.foodGram())
                        .conditionText(f.conditionText())
                        .build())
                .toList();
    }

    private List<StoolRecord> toStools(List<StoolRequest> requests) {
        return Optional.ofNullable(requests).orElseGet(List::of).stream()
                .map(s -> StoolRecord.builder()
                        .stoolTime(s.stoolTime())
                        .conditionText(s.conditionText())
                        .abnormal(s.abnormal())
                        .build())
                .toList();
    }
}
