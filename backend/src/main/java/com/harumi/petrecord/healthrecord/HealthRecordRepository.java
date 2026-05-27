package com.harumi.petrecord.healthrecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    Optional<HealthRecord> findByIdAndPetId(Long id, Long petId);

    List<HealthRecord> findAllByPetIdOrderByVisitDateDescIdDesc(Long petId);
}
