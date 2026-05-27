package com.harumi.petrecord.healthrecord.dto;

import com.harumi.petrecord.file.FileCategory;
import com.harumi.petrecord.file.FileResource;
import com.harumi.petrecord.healthrecord.HealthRecord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record HealthRecordResponse(
        Long id,
        LocalDate visitDate,
        String hospitalName,
        String doctorName,
        String medicalNote,
        List<AttachmentResponse> attachments,
        Instant createdAt,
        Instant updatedAt
) {
    public record AttachmentResponse(
            Long fileId,
            FileCategory category,
            String contentType,
            String originalFilename,
            Long fileSize
    ) {
        static AttachmentResponse from(FileResource f) {
            return new AttachmentResponse(f.getId(), f.getFileCategory(),
                    f.getContentType(), f.getOriginalFilename(), f.getFileSize());
        }
    }

    public static HealthRecordResponse from(HealthRecord hr, Map<Long, FileResource> filesById) {
        List<AttachmentResponse> atts = hr.getAttachments().stream()
                .map(a -> filesById.get(a.getFileId()))
                .filter(Objects::nonNull)
                .map(AttachmentResponse::from)
                .toList();
        return new HealthRecordResponse(
                hr.getId(),
                hr.getVisitDate(),
                hr.getHospitalName(),
                hr.getDoctorName(),
                hr.getMedicalNote(),
                atts,
                hr.getCreatedAt(),
                hr.getUpdatedAt()
        );
    }
}
