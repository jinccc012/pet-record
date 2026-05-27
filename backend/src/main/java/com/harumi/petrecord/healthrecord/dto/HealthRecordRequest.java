package com.harumi.petrecord.healthrecord.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

// Shared shape used for both create (POST) and full-replace (PUT).
public record HealthRecordRequest(
        @NotNull @PastOrPresent LocalDate visitDate,
        @Size(max = 255) String hospitalName,
        @Size(max = 255) String doctorName,
        @Size(max = 5000) String medicalNote,
        List<Long> attachedFileIds
) {
}
