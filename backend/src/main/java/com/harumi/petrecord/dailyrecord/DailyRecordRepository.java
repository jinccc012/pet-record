package com.harumi.petrecord.dailyrecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {

    Optional<DailyRecord> findByIdAndPetId(Long id, Long petId);

    List<DailyRecord> findAllByPetIdOrderByRecordDateDesc(Long petId);

    Optional<DailyRecord> findByPetIdAndRecordDate(Long petId, LocalDate recordDate);

    boolean existsByPetIdAndRecordDate(Long petId, LocalDate recordDate);
}
