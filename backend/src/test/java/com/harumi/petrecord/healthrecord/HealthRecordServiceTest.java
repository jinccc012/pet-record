package com.harumi.petrecord.healthrecord;

import com.harumi.petrecord.common.exception.ResourceNotFoundException;
import com.harumi.petrecord.file.FileCategory;
import com.harumi.petrecord.file.FileRepository;
import com.harumi.petrecord.file.FileResource;
import com.harumi.petrecord.file.FileStatus;
import com.harumi.petrecord.healthrecord.dto.HealthRecordRequest;
import com.harumi.petrecord.healthrecord.dto.HealthRecordResponse;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceTest {

    @Mock HealthRecordRepository healthRecordRepository;
    @Mock FileRepository fileRepository;
    @Mock PetRepository petRepository;

    @InjectMocks HealthRecordService service;

    private static final CurrentUser OWNER = new CurrentUser(1L, "alice", UserRole.USER);
    private static final CurrentUser STRANGER = new CurrentUser(2L, "bob", UserRole.USER);

    private void petOwnedBy(CurrentUser user) {
        when(petRepository.findByIdAndOwnerId(10L, user.id()))
                .thenReturn(Optional.of(Pet.builder().id(10L).ownerId(user.id())
                        .name("Mochi").species(PetSpecies.CAT).build()));
    }

    private FileResource ownedFile(Long fileId, FileCategory category) {
        return FileResource.builder()
                .id(fileId).uploadedBy(OWNER.id()).fileCategory(category)
                .originalFilename("x").storedFilename("x").storageProvider("R2")
                .bucketName("b").objectKey("k").contentType("application/pdf")
                .fileSize(100L).status(FileStatus.ACTIVE).build();
    }

    @Test
    void createPersistsHealthRecordWithAttachments() {
        petOwnedBy(OWNER);
        when(fileRepository.findByIdAndUploadedBy(100L, OWNER.id()))
                .thenReturn(Optional.of(ownedFile(100L, FileCategory.HEALTH_REPORT)));
        when(fileRepository.findByIdAndUploadedBy(101L, OWNER.id()))
                .thenReturn(Optional.of(ownedFile(101L, FileCategory.HEALTH_IMAGE)));
        when(fileRepository.findAllById(any())).thenReturn(List.of(
                ownedFile(100L, FileCategory.HEALTH_REPORT),
                ownedFile(101L, FileCategory.HEALTH_IMAGE)));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(inv -> {
            HealthRecord hr = inv.getArgument(0);
            hr.setId(500L);
            return hr;
        });

        HealthRecordRequest req = new HealthRecordRequest(
                LocalDate.of(2026, 5, 25), "Mochi Vet", "Dr. X", "Annual checkup",
                List.of(100L, 101L));

        HealthRecordResponse res = service.create(OWNER, 10L, req);

        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(captor.capture());
        HealthRecord saved = captor.getValue();
        assertThat(saved.getPetId()).isEqualTo(10L);
        assertThat(saved.getAttachments()).hasSize(2);
        assertThat(res.id()).isEqualTo(500L);
        assertThat(res.attachments()).hasSize(2);
        assertThat(res.attachments().get(0).fileId()).isIn(100L, 101L);
    }

    @Test
    void createRejectsWhenPetNotOwned() {
        when(petRepository.findByIdAndOwnerId(10L, STRANGER.id())).thenReturn(Optional.empty());
        HealthRecordRequest req = new HealthRecordRequest(
                LocalDate.of(2026, 5, 25), null, null, null, List.of());
        assertThatThrownBy(() -> service.create(STRANGER, 10L, req))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(healthRecordRepository, never()).save(any());
    }

    @Test
    void createRejectsFileNotOwnedByUser() {
        petOwnedBy(OWNER);
        when(fileRepository.findByIdAndUploadedBy(999L, OWNER.id())).thenReturn(Optional.empty());
        HealthRecordRequest req = new HealthRecordRequest(
                LocalDate.of(2026, 5, 25), null, null, null, List.of(999L));
        assertThatThrownBy(() -> service.create(OWNER, 10L, req))
                .isInstanceOf(IllegalArgumentException.class);
        verify(healthRecordRepository, never()).save(any());
    }

    @Test
    void updateReplacesFieldsAndAttachments() {
        petOwnedBy(OWNER);
        HealthRecord existing = HealthRecord.builder()
                .id(500L).petId(10L).visitDate(LocalDate.of(2026, 5, 25))
                .hospitalName("Old").build();
        existing.replaceAttachments(List.of(HealthRecordFile.builder().fileId(100L).build()));
        when(healthRecordRepository.findByIdAndPetId(500L, 10L)).thenReturn(Optional.of(existing));
        when(fileRepository.findByIdAndUploadedBy(102L, OWNER.id()))
                .thenReturn(Optional.of(ownedFile(102L, FileCategory.HEALTH_IMAGE)));
        when(fileRepository.findAllById(any())).thenReturn(List.of(
                ownedFile(102L, FileCategory.HEALTH_IMAGE)));

        HealthRecordRequest req = new HealthRecordRequest(
                LocalDate.of(2026, 5, 26), "New Hospital", "Dr. Y", "Follow-up",
                List.of(102L));
        HealthRecordResponse res = service.update(OWNER, 10L, 500L, req);

        assertThat(existing.getVisitDate()).isEqualTo(LocalDate.of(2026, 5, 26));
        assertThat(existing.getHospitalName()).isEqualTo("New Hospital");
        assertThat(existing.getAttachments()).hasSize(1);
        assertThat(existing.getAttachments().get(0).getFileId()).isEqualTo(102L);
        assertThat(res.attachments()).hasSize(1);
    }

    @Test
    void getMissingRecordThrows() {
        petOwnedBy(OWNER);
        when(healthRecordRepository.findByIdAndPetId(999L, 10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(OWNER, 10L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteSoftDeletesOwnedRecord() {
        petOwnedBy(OWNER);
        HealthRecord hr = HealthRecord.builder().id(500L).petId(10L).visitDate(LocalDate.now()).build();
        when(healthRecordRepository.findByIdAndPetId(500L, 10L)).thenReturn(Optional.of(hr));

        service.delete(OWNER, 10L, 500L);

        verify(healthRecordRepository).delete(hr);
    }
}
