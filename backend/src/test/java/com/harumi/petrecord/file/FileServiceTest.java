package com.harumi.petrecord.file;

import com.harumi.petrecord.common.exception.ResourceNotFoundException;
import com.harumi.petrecord.file.dto.CompleteUploadRequest;
import com.harumi.petrecord.file.dto.CompleteUploadResponse;
import com.harumi.petrecord.file.dto.SignedUploadUrlRequest;
import com.harumi.petrecord.file.dto.SignedUploadUrlResponse;
import com.harumi.petrecord.pet.Pet;
import com.harumi.petrecord.pet.PetRepository;
import com.harumi.petrecord.pet.PetSpecies;
import com.harumi.petrecord.security.CurrentUser;
import com.harumi.petrecord.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock UploadSessionRepository uploadSessionRepository;
    @Mock FileRepository fileRepository;
    @Mock PetRepository petRepository;
    @Mock R2StorageService storage;
    @Mock R2Properties properties;

    @InjectMocks FileService fileService;

    private static final CurrentUser OWNER = new CurrentUser(1L, "alice", UserRole.USER);
    private static final CurrentUser STRANGER = new CurrentUser(2L, "bob", UserRole.USER);

    @BeforeEach
    void stubProps() {
        lenient().when(properties.getBucketName()).thenReturn("pet-record-files");
        lenient().when(properties.getUploadExpireMinutes()).thenReturn(10);
        lenient().when(properties.getDownloadExpireMinutes()).thenReturn(15);
    }

    private void petOwnedBy(CurrentUser user) {
        lenient().when(petRepository.findByIdAndOwnerId(10L, user.id()))
                .thenReturn(Optional.of(Pet.builder().id(10L).ownerId(user.id())
                        .name("Mochi").species(PetSpecies.CAT).build()));
    }

    private SignedUploadUrlRequest avatarReq(String contentType, long size) {
        return new SignedUploadUrlRequest(10L, FileCategory.AVATAR, "photo.jpg", contentType, size);
    }

    @Test
    void createSignedUploadUrlGeneratesScopedKeyAndPendingSession() {
        petOwnedBy(OWNER);
        when(uploadSessionRepository.save(any(UploadSession.class))).thenAnswer(inv -> {
            UploadSession s = inv.getArgument(0);
            s.setId(50L);
            return s;
        });
        when(storage.presignUploadUrl(anyString(), anyString(), any(Duration.class)))
                .thenReturn("https://r2/presigned-put");

        SignedUploadUrlResponse res = fileService.createSignedUploadUrl(OWNER, avatarReq("image/jpeg", 1024));

        ArgumentCaptor<UploadSession> captor = ArgumentCaptor.forClass(UploadSession.class);
        verify(uploadSessionRepository).save(captor.capture());
        UploadSession saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(UploadSessionStatus.PENDING);
        assertThat(saved.getObjectKey()).startsWith("avatars/users/1/pets/10/").endsWith(".jpg");
        assertThat(res.uploadSessionId()).isEqualTo(50L);
        assertThat(res.uploadUrl()).isEqualTo("https://r2/presigned-put");
        assertThat(res.expiresInSeconds()).isEqualTo(600L);
    }

    @Test
    void createRejectsNonAvatarCategory() {
        SignedUploadUrlRequest req = new SignedUploadUrlRequest(
                10L, FileCategory.HEALTH_REPORT, "x.pdf", "application/pdf", 100L);
        assertThatThrownBy(() -> fileService.createSignedUploadUrl(OWNER, req))
                .isInstanceOf(IllegalArgumentException.class);
        verify(uploadSessionRepository, never()).save(any());
    }

    @Test
    void createRejectsWhenPetNotOwned() {
        when(petRepository.findByIdAndOwnerId(10L, STRANGER.id())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> fileService.createSignedUploadUrl(STRANGER, avatarReq("image/jpeg", 1024)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createRejectsBadContentType() {
        petOwnedBy(OWNER);
        assertThatThrownBy(() -> fileService.createSignedUploadUrl(OWNER, avatarReq("image/gif", 1024)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsTooLargeFile() {
        petOwnedBy(OWNER);
        assertThatThrownBy(() -> fileService.createSignedUploadUrl(OWNER, avatarReq("image/png", 6L * 1024 * 1024)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void completeUploadCreatesFileAndSetsAvatar() {
        UploadSession session = UploadSession.builder()
                .id(50L).userId(OWNER.id()).petId(10L).fileCategory(FileCategory.AVATAR)
                .originalFilename("photo.jpg").storedFilename("uuid.jpg")
                .bucketName("pet-record-files").objectKey("avatars/users/1/pets/10/uuid.jpg")
                .contentType("image/jpeg").fileSize(1024L)
                .status(UploadSessionStatus.PENDING).expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(uploadSessionRepository.findByIdAndUserId(50L, OWNER.id())).thenReturn(Optional.of(session));
        when(storage.objectExists(session.getObjectKey())).thenReturn(true);
        when(fileRepository.save(any(FileResource.class))).thenAnswer(inv -> {
            FileResource f = inv.getArgument(0);
            f.setId(100L);
            return f;
        });
        Pet pet = Pet.builder().id(10L).ownerId(OWNER.id()).name("Mochi").species(PetSpecies.CAT).build();
        when(petRepository.findByIdAndOwnerId(10L, OWNER.id())).thenReturn(Optional.of(pet));

        CompleteUploadResponse res = fileService.completeUpload(OWNER, new CompleteUploadRequest(50L));

        assertThat(res.fileId()).isEqualTo(100L);
        assertThat(session.getStatus()).isEqualTo(UploadSessionStatus.COMPLETED);
        assertThat(session.getCompletedAt()).isNotNull();
        assertThat(pet.getAvatarFileId()).isEqualTo(100L);
    }

    @Test
    void completeUploadMissingSessionThrows() {
        when(uploadSessionRepository.findByIdAndUserId(99L, OWNER.id())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> fileService.completeUpload(OWNER, new CompleteUploadRequest(99L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void completeUploadNotPendingThrows() {
        UploadSession session = UploadSession.builder()
                .id(50L).userId(OWNER.id()).status(UploadSessionStatus.COMPLETED)
                .expiresAt(Instant.now().plusSeconds(300)).build();
        when(uploadSessionRepository.findByIdAndUserId(50L, OWNER.id())).thenReturn(Optional.of(session));
        assertThatThrownBy(() -> fileService.completeUpload(OWNER, new CompleteUploadRequest(50L)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(fileRepository, never()).save(any());
    }

    @Test
    void completeUploadObjectMissingMarksFailed() {
        UploadSession session = UploadSession.builder()
                .id(50L).userId(OWNER.id()).petId(10L).fileCategory(FileCategory.AVATAR)
                .objectKey("avatars/users/1/pets/10/uuid.jpg").status(UploadSessionStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(300)).build();
        when(uploadSessionRepository.findByIdAndUserId(50L, OWNER.id())).thenReturn(Optional.of(session));
        when(storage.objectExists(session.getObjectKey())).thenReturn(false);

        assertThatThrownBy(() -> fileService.completeUpload(OWNER, new CompleteUploadRequest(50L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(session.getStatus()).isEqualTo(UploadSessionStatus.FAILED);
        verify(fileRepository, never()).save(any());
    }

    @Test
    void signedViewUrlReturnsUrlForOwnedFile() {
        FileResource file = FileResource.builder()
                .id(100L).uploadedBy(OWNER.id()).objectKey("avatars/users/1/pets/10/uuid.jpg").build();
        when(fileRepository.findByIdAndUploadedBy(100L, OWNER.id())).thenReturn(Optional.of(file));
        when(storage.presignDownloadUrl(anyString(), any(Duration.class))).thenReturn("https://r2/presigned-get");

        var res = fileService.createSignedViewUrl(OWNER, 100L);

        assertThat(res.url()).isEqualTo("https://r2/presigned-get");
        assertThat(res.expiresInSeconds()).isEqualTo(900L);
    }

    @Test
    void signedViewUrlMissingFileThrows() {
        when(fileRepository.findByIdAndUploadedBy(100L, STRANGER.id())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> fileService.createSignedViewUrl(STRANGER, 100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
