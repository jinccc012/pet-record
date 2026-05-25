package com.harumi.petrecord.dailyrecord;

import com.harumi.petrecord.common.exception.DuplicateResourceException;
import com.harumi.petrecord.common.exception.ResourceNotFoundException;
import com.harumi.petrecord.dailyrecord.dto.CreateDailyRecordRequest;
import com.harumi.petrecord.dailyrecord.dto.DailyRecordResponse;
import com.harumi.petrecord.dailyrecord.dto.FeedingRequest;
import com.harumi.petrecord.dailyrecord.dto.StoolRequest;
import com.harumi.petrecord.dailyrecord.dto.UpdateDailyRecordRequest;
import com.harumi.petrecord.pet.Pet;
import com.harumi.petrecord.pet.PetRepository;
import com.harumi.petrecord.pet.PetSpecies;
import com.harumi.petrecord.security.CurrentUser;
import com.harumi.petrecord.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyRecordServiceTest {

    @Mock DailyRecordRepository dailyRecordRepository;
    @Mock PetRepository petRepository;

    @InjectMocks DailyRecordService service;

    private static final CurrentUser OWNER = new CurrentUser(1L, "alice", UserRole.USER);
    private static final CurrentUser STRANGER = new CurrentUser(2L, "bob", UserRole.USER);
    private static final LocalDate DATE = LocalDate.of(2026, 5, 25);

    private void petOwnedBy(CurrentUser user) {
        when(petRepository.findByIdAndOwnerId(10L, user.id()))
                .thenReturn(Optional.of(Pet.builder().id(10L).ownerId(user.id())
                        .name("Mochi").species(PetSpecies.CAT).build()));
    }

    @Test
    void createPersistsRecordWithChildren() {
        petOwnedBy(OWNER);
        when(dailyRecordRepository.existsByPetIdAndRecordDate(10L, DATE)).thenReturn(false);
        when(dailyRecordRepository.save(any(DailyRecord.class))).thenAnswer(inv -> {
            DailyRecord d = inv.getArgument(0);
            d.setId(100L);
            return d;
        });

        CreateDailyRecordRequest req = new CreateDailyRecordRequest(
                DATE, new BigDecimal("5.20"), 300, "good day",
                List.of(new FeedingRequest(LocalTime.of(8, 0), 120, "dry food")),
                List.of(new StoolRequest(LocalTime.of(9, 0), "normal", false)));

        DailyRecordResponse res = service.create(OWNER, 10L, req);

        ArgumentCaptor<DailyRecord> captor = ArgumentCaptor.forClass(DailyRecord.class);
        verify(dailyRecordRepository).save(captor.capture());
        DailyRecord saved = captor.getValue();
        assertThat(saved.getPetId()).isEqualTo(10L);
        assertThat(saved.getRecordDate()).isEqualTo(DATE);
        assertThat(saved.getFeedings()).hasSize(1);
        assertThat(saved.getStools()).hasSize(1);
        assertThat(res.id()).isEqualTo(100L);
        assertThat(res.feedings()).hasSize(1);
        assertThat(res.feedings().get(0).foodGram()).isEqualTo(120);
        assertThat(res.stools().get(0).abnormal()).isFalse();
    }

    @Test
    void createRejectsDuplicateDate() {
        petOwnedBy(OWNER);
        when(dailyRecordRepository.existsByPetIdAndRecordDate(10L, DATE)).thenReturn(true);

        CreateDailyRecordRequest req = new CreateDailyRecordRequest(DATE, null, null, null, null, null);
        assertThatThrownBy(() -> service.create(OWNER, 10L, req))
                .isInstanceOf(DuplicateResourceException.class);
        verify(dailyRecordRepository, never()).save(any());
    }

    @Test
    void createRejectsWhenPetNotOwned() {
        when(petRepository.findByIdAndOwnerId(10L, STRANGER.id())).thenReturn(Optional.empty());

        CreateDailyRecordRequest req = new CreateDailyRecordRequest(DATE, null, null, null, null, null);
        assertThatThrownBy(() -> service.create(STRANGER, 10L, req))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(dailyRecordRepository, never()).save(any());
    }

    @Test
    void getReturnsOwnedRecord() {
        petOwnedBy(OWNER);
        when(dailyRecordRepository.findByIdAndPetId(100L, 10L))
                .thenReturn(Optional.of(DailyRecord.builder().id(100L).petId(10L).recordDate(DATE).build()));

        assertThat(service.get(OWNER, 10L, 100L).id()).isEqualTo(100L);
    }

    @Test
    void getMissingRecordThrows() {
        petOwnedBy(OWNER);
        when(dailyRecordRepository.findByIdAndPetId(999L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(OWNER, 10L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateReplacesChildren() {
        petOwnedBy(OWNER);
        DailyRecord existing = DailyRecord.builder().id(100L).petId(10L).recordDate(DATE).build();
        existing.replaceFeedings(List.of(FeedingRecord.builder().feedingTime(LocalTime.of(7, 0)).build()));
        when(dailyRecordRepository.findByIdAndPetId(100L, 10L)).thenReturn(Optional.of(existing));

        UpdateDailyRecordRequest req = new UpdateDailyRecordRequest(
                new BigDecimal("6.00"), 350, "updated",
                List.of(new FeedingRequest(LocalTime.of(18, 0), 200, "wet food"),
                        new FeedingRequest(LocalTime.of(20, 0), 50, "treat")),
                List.of());

        DailyRecordResponse res = service.update(OWNER, 10L, 100L, req);

        assertThat(res.weightKg()).isEqualByComparingTo("6.00");
        assertThat(res.feedings()).hasSize(2);
        assertThat(res.stools()).isEmpty();
        assertThat(existing.getFeedings()).hasSize(2);
    }

    @Test
    void deleteSoftDeletesOwnedRecord() {
        petOwnedBy(OWNER);
        DailyRecord record = DailyRecord.builder().id(100L).petId(10L).recordDate(DATE).build();
        when(dailyRecordRepository.findByIdAndPetId(100L, 10L)).thenReturn(Optional.of(record));

        service.delete(OWNER, 10L, 100L);

        verify(dailyRecordRepository).delete(record);
    }

    @Test
    void listByDateReturnsSingleOrEmpty() {
        petOwnedBy(OWNER);
        when(dailyRecordRepository.findByPetIdAndRecordDate(10L, DATE))
                .thenReturn(Optional.of(DailyRecord.builder().id(100L).petId(10L).recordDate(DATE).build()));

        assertThat(service.list(OWNER, 10L, DATE)).hasSize(1);

        when(dailyRecordRepository.findByPetIdAndRecordDate(eq(10L), eq(LocalDate.of(2000, 1, 1))))
                .thenReturn(Optional.empty());
        assertThat(service.list(OWNER, 10L, LocalDate.of(2000, 1, 1))).isEmpty();
    }

    @Test
    void chartMapsRowsToAlignedArrays() {
        petOwnedBy(OWNER);
        LocalDate from = LocalDate.of(2026, 5, 20);
        LocalDate to = LocalDate.of(2026, 5, 21);
        when(dailyRecordRepository.findChartRows(10L, from, to)).thenReturn(List.of(
                new DailyChartRow(from, new BigDecimal("5.20"), 300, 120L),
                new DailyChartRow(to, new BigDecimal("5.30"), 280, 130L)));

        var res = service.chart(OWNER, 10L, from, to);

        assertThat(res.labels()).containsExactly(from, to);
        assertThat(res.weightKg()).containsExactly(new BigDecimal("5.20"), new BigDecimal("5.30"));
        assertThat(res.waterMl()).containsExactly(300, 280);
        assertThat(res.foodGram()).containsExactly(120L, 130L);
    }

    @Test
    void chartRejectsFromAfterTo() {
        petOwnedBy(OWNER);
        assertThatThrownBy(() -> service.chart(OWNER, 10L, LocalDate.of(2026, 5, 21), LocalDate.of(2026, 5, 20)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void chartRejectsWhenPetNotOwned() {
        when(petRepository.findByIdAndOwnerId(10L, STRANGER.id())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.chart(STRANGER, 10L, DATE, DATE))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
