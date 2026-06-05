package com.harumi.petrecord.healthrecord;

import com.harumi.petrecord.common.exception.ResourceNotFoundException;
import com.harumi.petrecord.file.FileRepository;
import com.harumi.petrecord.file.FileResource;
import com.harumi.petrecord.healthrecord.dto.HealthRecordRequest;
import com.harumi.petrecord.healthrecord.dto.HealthRecordResponse;
import com.harumi.petrecord.pet.PetRepository;
import com.harumi.petrecord.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HealthRecordService {

    private static final Logger log = LoggerFactory.getLogger(HealthRecordService.class);

    private final HealthRecordRepository healthRecordRepository;
    private final FileRepository fileRepository;
    private final PetRepository petRepository;

    public HealthRecordService(HealthRecordRepository healthRecordRepository,
                               FileRepository fileRepository,
                               PetRepository petRepository) {
        this.healthRecordRepository = healthRecordRepository;
        this.fileRepository = fileRepository;
        this.petRepository = petRepository;
    }

    @Transactional
    public HealthRecordResponse create(CurrentUser currentUser, Long petId, HealthRecordRequest request) {
        assertPetOwned(currentUser, petId);
        List<Long> fileIds = sanitizeFileIds(request.attachedFileIds());
        validateFilesOwned(currentUser, fileIds);

        HealthRecord hr = HealthRecord.builder()
                .petId(petId)
                .visitDate(request.visitDate())
                .hospitalName(emptyToNull(request.hospitalName()))
                .doctorName(emptyToNull(request.doctorName()))
                .medicalNote(emptyToNull(request.medicalNote()))
                .build();
        hr.replaceAttachments(toAttachments(fileIds));
        HealthRecord saved = healthRecordRepository.save(hr);
        log.info("Created health record id={} petId={} attachments={}",
                saved.getId(), petId, fileIds.size());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<HealthRecordResponse> list(CurrentUser currentUser, Long petId) {
        assertPetOwned(currentUser, petId);
        List<HealthRecord> records = healthRecordRepository.findAllByPetIdOrderByVisitDateDescIdDesc(petId);
        Map<Long, FileResource> filesById = loadAttachmentFiles(records);
        return records.stream().map(hr -> HealthRecordResponse.from(hr, filesById)).toList();
    }

    @Transactional(readOnly = true)
    public HealthRecordResponse get(CurrentUser currentUser, Long petId, Long recordId) {
        assertPetOwned(currentUser, petId);
        return toResponse(loadOwned(petId, recordId));
    }

    @Transactional
    public HealthRecordResponse update(CurrentUser currentUser, Long petId, Long recordId,
                                       HealthRecordRequest request) {
        assertPetOwned(currentUser, petId);
        HealthRecord hr = loadOwned(petId, recordId);
        List<Long> fileIds = sanitizeFileIds(request.attachedFileIds());
        validateFilesOwned(currentUser, fileIds);

        hr.setVisitDate(request.visitDate());
        hr.setHospitalName(emptyToNull(request.hospitalName()));
        hr.setDoctorName(emptyToNull(request.doctorName()));
        hr.setMedicalNote(emptyToNull(request.medicalNote()));
        hr.replaceAttachments(toAttachments(fileIds));
        log.info("Updated health record id={} petId={} attachments={}",
                recordId, petId, fileIds.size());
        return toResponse(hr);
    }

    @Transactional
    public void delete(CurrentUser currentUser, Long petId, Long recordId) {
        assertPetOwned(currentUser, petId);
        HealthRecord hr = loadOwned(petId, recordId);
        healthRecordRepository.delete(hr);
        log.info("Soft-deleted health record id={} petId={}", recordId, petId);
    }

    private void assertPetOwned(CurrentUser currentUser, Long petId) {
        petRepository.findByIdAndOwnerId(petId, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
    }

    private HealthRecord loadOwned(Long petId, Long recordId) {
        return healthRecordRepository.findByIdAndPetId(recordId, petId)
                .orElseThrow(() -> new ResourceNotFoundException("Health record not found"));
    }

    private List<Long> sanitizeFileIds(List<Long> raw) {
        if (raw == null) return List.of();
        return raw.stream().filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf));
    }

    private void validateFilesOwned(CurrentUser currentUser, List<Long> fileIds) {
        for (Long fileId : fileIds) {
            fileRepository.findByIdAndUploadedBy(fileId, currentUser.id())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Attachment file not owned or not found: " + fileId));
        }
    }

    private List<HealthRecordFile> toAttachments(List<Long> fileIds) {
        return fileIds.stream()
                .map(id -> HealthRecordFile.builder().fileId(id).build())
                .toList();
    }

    private Map<Long, FileResource> loadAttachmentFiles(List<HealthRecord> records) {
        List<Long> fileIds = records.stream()
                .flatMap(hr -> hr.getAttachments().stream())
                .map(HealthRecordFile::getFileId)
                .distinct()
                .toList();
        if (fileIds.isEmpty()) return Map.of();
        return fileRepository.findAllById(fileIds).stream()
                .collect(Collectors.toMap(FileResource::getId, Function.identity()));
    }

    private HealthRecordResponse toResponse(HealthRecord hr) {
        return HealthRecordResponse.from(hr, loadAttachmentFiles(List.of(hr)));
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
