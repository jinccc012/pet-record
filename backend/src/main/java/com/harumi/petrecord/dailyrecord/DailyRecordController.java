package com.harumi.petrecord.dailyrecord;

import com.harumi.petrecord.dailyrecord.dto.ChartResponse;
import com.harumi.petrecord.dailyrecord.dto.CreateDailyRecordRequest;
import com.harumi.petrecord.dailyrecord.dto.DailyRecordResponse;
import com.harumi.petrecord.dailyrecord.dto.UpdateDailyRecordRequest;
import com.harumi.petrecord.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pets/{petId}/daily-records")
public class DailyRecordController {

    private final DailyRecordService dailyRecordService;

    public DailyRecordController(DailyRecordService dailyRecordService) {
        this.dailyRecordService = dailyRecordService;
    }

    @PostMapping
    public ResponseEntity<DailyRecordResponse> create(@AuthenticationPrincipal CurrentUser currentUser,
                                                      @PathVariable Long petId,
                                                      @Valid @RequestBody CreateDailyRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dailyRecordService.create(currentUser, petId, request));
    }

    @GetMapping
    public ResponseEntity<List<DailyRecordResponse>> list(@AuthenticationPrincipal CurrentUser currentUser,
                                                          @PathVariable Long petId,
                                                          @RequestParam(required = false)
                                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(dailyRecordService.list(currentUser, petId, date));
    }

    @GetMapping("/chart")
    public ResponseEntity<ChartResponse> chart(@AuthenticationPrincipal CurrentUser currentUser,
                                               @PathVariable Long petId,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(dailyRecordService.chart(currentUser, petId, from, to));
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<DailyRecordResponse> get(@AuthenticationPrincipal CurrentUser currentUser,
                                                   @PathVariable Long petId,
                                                   @PathVariable Long recordId) {
        return ResponseEntity.ok(dailyRecordService.get(currentUser, petId, recordId));
    }

    @PutMapping("/{recordId}")
    public ResponseEntity<DailyRecordResponse> update(@AuthenticationPrincipal CurrentUser currentUser,
                                                      @PathVariable Long petId,
                                                      @PathVariable Long recordId,
                                                      @Valid @RequestBody UpdateDailyRecordRequest request) {
        return ResponseEntity.ok(dailyRecordService.update(currentUser, petId, recordId, request));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser currentUser,
                                       @PathVariable Long petId,
                                       @PathVariable Long recordId) {
        dailyRecordService.delete(currentUser, petId, recordId);
        return ResponseEntity.noContent().build();
    }
}
