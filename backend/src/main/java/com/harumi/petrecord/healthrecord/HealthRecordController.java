package com.harumi.petrecord.healthrecord;

import com.harumi.petrecord.healthrecord.dto.HealthRecordRequest;
import com.harumi.petrecord.healthrecord.dto.HealthRecordResponse;
import com.harumi.petrecord.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pets/{petId}/health-records")
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    public HealthRecordController(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

    @PostMapping
    public ResponseEntity<HealthRecordResponse> create(@AuthenticationPrincipal CurrentUser currentUser,
                                                       @PathVariable Long petId,
                                                       @Valid @RequestBody HealthRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(healthRecordService.create(currentUser, petId, request));
    }

    @GetMapping
    public ResponseEntity<List<HealthRecordResponse>> list(@AuthenticationPrincipal CurrentUser currentUser,
                                                           @PathVariable Long petId) {
        return ResponseEntity.ok(healthRecordService.list(currentUser, petId));
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<HealthRecordResponse> get(@AuthenticationPrincipal CurrentUser currentUser,
                                                    @PathVariable Long petId,
                                                    @PathVariable Long recordId) {
        return ResponseEntity.ok(healthRecordService.get(currentUser, petId, recordId));
    }

    @PutMapping("/{recordId}")
    public ResponseEntity<HealthRecordResponse> update(@AuthenticationPrincipal CurrentUser currentUser,
                                                       @PathVariable Long petId,
                                                       @PathVariable Long recordId,
                                                       @Valid @RequestBody HealthRecordRequest request) {
        return ResponseEntity.ok(healthRecordService.update(currentUser, petId, recordId, request));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser currentUser,
                                       @PathVariable Long petId,
                                       @PathVariable Long recordId) {
        healthRecordService.delete(currentUser, petId, recordId);
        return ResponseEntity.noContent().build();
    }
}
