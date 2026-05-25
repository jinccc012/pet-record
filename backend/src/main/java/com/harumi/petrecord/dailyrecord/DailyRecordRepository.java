package com.harumi.petrecord.dailyrecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            SELECT new com.harumi.petrecord.dailyrecord.DailyChartRow(
                d.recordDate, d.weightKg, d.waterMl, COALESCE(SUM(f.foodGram), 0L))
            FROM DailyRecord d LEFT JOIN d.feedings f
            WHERE d.petId = :petId AND d.recordDate BETWEEN :from AND :to
            GROUP BY d.id, d.recordDate, d.weightKg, d.waterMl
            ORDER BY d.recordDate ASC
            """)
    List<DailyChartRow> findChartRows(@Param("petId") Long petId,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);
}
