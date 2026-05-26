package com.harumi.petrecord.file;

import com.harumi.petrecord.file.dto.CompleteUploadRequest;
import com.harumi.petrecord.file.dto.CompleteUploadResponse;
import com.harumi.petrecord.file.dto.SignedUploadUrlRequest;
import com.harumi.petrecord.file.dto.SignedUploadUrlResponse;
import com.harumi.petrecord.file.dto.SignedViewUrlResponse;
import com.harumi.petrecord.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/signed-upload-url")
    public ResponseEntity<SignedUploadUrlResponse> signedUploadUrl(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody SignedUploadUrlRequest request) {
        return ResponseEntity.ok(fileService.createSignedUploadUrl(currentUser, request));
    }

    @PostMapping("/complete-upload")
    public ResponseEntity<CompleteUploadResponse> completeUpload(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CompleteUploadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.completeUpload(currentUser, request));
    }

    @GetMapping("/{fileId}/signed-view-url")
    public ResponseEntity<SignedViewUrlResponse> signedViewUrl(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long fileId) {
        return ResponseEntity.ok(fileService.createSignedViewUrl(currentUser, fileId));
    }
}
