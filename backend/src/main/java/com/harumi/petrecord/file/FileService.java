package com.harumi.petrecord.file;

import com.harumi.petrecord.common.exception.ResourceNotFoundException;
import com.harumi.petrecord.file.dto.CompleteUploadRequest;
import com.harumi.petrecord.file.dto.CompleteUploadResponse;
import com.harumi.petrecord.file.dto.SignedUploadUrlRequest;
import com.harumi.petrecord.file.dto.SignedUploadUrlResponse;
import com.harumi.petrecord.file.dto.SignedViewUrlResponse;
import com.harumi.petrecord.pet.Pet;
import com.harumi.petrecord.pet.PetRepository;
import com.harumi.petrecord.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );
    private static final Set<String> ALLOWED_AVATAR_TYPES = IMAGE_EXTENSIONS.keySet();
    private static final String PROVIDER_R2 = "R2";

    private final UploadSessionRepository uploadSessionRepository;
    private final FileRepository fileRepository;
    private final PetRepository petRepository;
    private final R2StorageService storage;
    private final R2Properties properties;

    public FileService(UploadSessionRepository uploadSessionRepository,
                       FileRepository fileRepository,
                       PetRepository petRepository,
                       R2StorageService storage,
                       R2Properties properties) {
        this.uploadSessionRepository = uploadSessionRepository;
        this.fileRepository = fileRepository;
        this.petRepository = petRepository;
        this.storage = storage;
        this.properties = properties;
    }

    @Transactional
    public SignedUploadUrlResponse createSignedUploadUrl(CurrentUser currentUser, SignedUploadUrlRequest request) {
        if (request.category() != FileCategory.AVATAR) {
            throw new IllegalArgumentException("Only AVATAR uploads are supported");
        }
        Pet pet = loadOwnedPet(currentUser, request.petId());
        validateAvatar(request.contentType(), request.fileSize());

        String ext = IMAGE_EXTENSIONS.get(request.contentType());
        String storedFilename = UUID.randomUUID() + "." + ext;
        String objectKey = "avatars/users/%d/pets/%d/%s".formatted(currentUser.id(), pet.getId(), storedFilename);

        UploadSession session = UploadSession.builder()
                .userId(currentUser.id())
                .petId(pet.getId())
                .fileCategory(FileCategory.AVATAR)
                .originalFilename(request.originalFilename())
                .storedFilename(storedFilename)
                .bucketName(properties.getBucketName())
                .objectKey(objectKey)
                .contentType(request.contentType())
                .fileSize(request.fileSize())
                .status(UploadSessionStatus.PENDING)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(properties.getUploadExpireMinutes())))
                .build();
        UploadSession saved = uploadSessionRepository.save(session);

        String uploadUrl = storage.presignUploadUrl(objectKey, request.contentType(),
                Duration.ofMinutes(properties.getUploadExpireMinutes()));
        log.info("Created upload session id={} petId={}", saved.getId(), pet.getId());
        return new SignedUploadUrlResponse(saved.getId(), uploadUrl, objectKey,
                properties.getUploadExpireMinutes() * 60L);
    }

    @Transactional
    public CompleteUploadResponse completeUpload(CurrentUser currentUser, CompleteUploadRequest request) {
        UploadSession session = uploadSessionRepository.findByIdAndUserId(request.uploadSessionId(), currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Upload session not found"));

        if (session.getStatus() != UploadSessionStatus.PENDING) {
            throw new IllegalArgumentException("Upload session is not pending");
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setStatus(UploadSessionStatus.EXPIRED);
            throw new IllegalArgumentException("Upload session expired");
        }
        if (!storage.objectExists(session.getObjectKey())) {
            session.setStatus(UploadSessionStatus.FAILED);
            throw new IllegalArgumentException("Uploaded object not found in storage");
        }

        FileResource file = fileRepository.save(FileResource.builder()
                .uploadSessionId(session.getId())
                .uploadedBy(currentUser.id())
                .originalFilename(session.getOriginalFilename())
                .storedFilename(session.getStoredFilename())
                .storageProvider(PROVIDER_R2)
                .bucketName(session.getBucketName())
                .objectKey(session.getObjectKey())
                .contentType(session.getContentType())
                .fileSize(session.getFileSize())
                .fileCategory(session.getFileCategory())
                .status(FileStatus.ACTIVE)
                .build());

        session.setStatus(UploadSessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());

        if (session.getFileCategory() == FileCategory.AVATAR && session.getPetId() != null) {
            Pet pet = loadOwnedPet(currentUser, session.getPetId());
            pet.setAvatarFileId(file.getId());
        }

        log.info("Completed upload session id={} fileId={}", session.getId(), file.getId());
        return new CompleteUploadResponse(file.getId(), file.getFileCategory(),
                file.getOriginalFilename(), file.getContentType(), file.getFileSize());
    }

    @Transactional(readOnly = true)
    public SignedViewUrlResponse createSignedViewUrl(CurrentUser currentUser, Long fileId) {
        FileResource file = fileRepository.findByIdAndUploadedBy(fileId, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        String url = storage.presignDownloadUrl(file.getObjectKey(),
                Duration.ofMinutes(properties.getDownloadExpireMinutes()));
        return new SignedViewUrlResponse(url, properties.getDownloadExpireMinutes() * 60L);
    }

    private Pet loadOwnedPet(CurrentUser currentUser, Long petId) {
        return petRepository.findByIdAndOwnerId(petId, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
    }

    private void validateAvatar(String contentType, long fileSize) {
        if (!ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported image type: " + contentType);
        }
        if (fileSize <= 0 || fileSize > MAX_AVATAR_BYTES) {
            throw new IllegalArgumentException("Avatar must be between 1 byte and 5 MB");
        }
    }
}
