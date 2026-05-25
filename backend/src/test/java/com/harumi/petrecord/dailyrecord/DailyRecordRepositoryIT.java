package com.harumi.petrecord.dailyrecord;

import com.harumi.petrecord.pet.Pet;
import com.harumi.petrecord.pet.PetRepository;
import com.harumi.petrecord.pet.PetSpecies;
import com.harumi.petrecord.user.User;
import com.harumi.petrecord.user.UserRepository;
import com.harumi.petrecord.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
class DailyRecordRepositoryIT {

    @Autowired DailyRecordRepository dailyRecordRepository;
    @Autowired PetRepository petRepository;
    @Autowired UserRepository userRepository;

    private Long petId(String suffix) {
        User owner = userRepository.save(User.builder()
                .username("owner" + suffix).email("owner" + suffix + "@example.com")
                .passwordHash("hash").role(UserRole.USER).build());
        Pet pet = petRepository.save(Pet.builder()
                .ownerId(owner.getId()).name("Mochi").species(PetSpecies.CAT).build());
        return pet.getId();
    }

    @Test
    void persistsRecordWithChildrenAndValidatesSchema() {
        Long pid = petId("a");
        DailyRecord record = DailyRecord.builder()
                .petId(pid).recordDate(LocalDate.of(2026, 5, 25))
                .weightKg(new BigDecimal("5.20")).waterMl(300).dailyNote("ok")
                .build();
        record.replaceFeedings(List.of(FeedingRecord.builder().feedingTime(LocalTime.of(8, 0)).foodGram(120).build()));
        record.replaceStools(List.of(StoolRecord.builder().stoolTime(LocalTime.of(9, 0)).abnormal(true).build()));

        DailyRecord saved = dailyRecordRepository.save(record);
        dailyRecordRepository.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        DailyRecord reloaded = dailyRecordRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getFeedings()).hasSize(1);
        assertThat(reloaded.getStools()).hasSize(1);
        assertThat(reloaded.getStools().get(0).isAbnormal()).isTrue();
    }

    @Test
    void updateReplacesChildrenViaOrphanRemoval() {
        Long pid = petId("b");
        DailyRecord record = DailyRecord.builder().petId(pid).recordDate(LocalDate.of(2026, 5, 25)).build();
        record.replaceFeedings(List.of(FeedingRecord.builder().feedingTime(LocalTime.of(8, 0)).build()));
        DailyRecord saved = dailyRecordRepository.saveAndFlush(record);

        saved.replaceFeedings(List.of(
                FeedingRecord.builder().feedingTime(LocalTime.of(18, 0)).foodGram(200).build(),
                FeedingRecord.builder().feedingTime(LocalTime.of(20, 0)).foodGram(50).build()));
        dailyRecordRepository.saveAndFlush(saved);

        DailyRecord reloaded = dailyRecordRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getFeedings()).hasSize(2);
    }

    @Test
    void softDeleteHidesRecord() {
        Long pid = petId("c");
        DailyRecord saved = dailyRecordRepository.saveAndFlush(
                DailyRecord.builder().petId(pid).recordDate(LocalDate.of(2026, 5, 25)).build());

        dailyRecordRepository.delete(saved);
        dailyRecordRepository.flush();

        assertThat(dailyRecordRepository.findById(saved.getId())).isEmpty();
        assertThat(dailyRecordRepository.findByIdAndPetId(saved.getId(), pid)).isEmpty();
        // After soft delete, the same date can be re-used (partial unique index)
        assertThat(dailyRecordRepository.existsByPetIdAndRecordDate(pid, LocalDate.of(2026, 5, 25))).isFalse();
    }

    @Test
    void findAllByPetScopesAndOrders() {
        Long pid = petId("d");
        dailyRecordRepository.save(DailyRecord.builder().petId(pid).recordDate(LocalDate.of(2026, 5, 20)).build());
        dailyRecordRepository.save(DailyRecord.builder().petId(pid).recordDate(LocalDate.of(2026, 5, 25)).build());
        dailyRecordRepository.flush();

        List<DailyRecord> all = dailyRecordRepository.findAllByPetIdOrderByRecordDateDesc(pid);
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getRecordDate()).isEqualTo(LocalDate.of(2026, 5, 25));
    }
}
