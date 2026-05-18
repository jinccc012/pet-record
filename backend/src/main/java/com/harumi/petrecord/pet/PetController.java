package com.harumi.petrecord.pet;

import com.harumi.petrecord.pet.dto.CreatePetRequest;
import com.harumi.petrecord.pet.dto.PetResponse;
import com.harumi.petrecord.pet.dto.UpdatePetRequest;
import com.harumi.petrecord.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @PostMapping
    public ResponseEntity<PetResponse> create(@AuthenticationPrincipal CurrentUser currentUser,
                                              @Valid @RequestBody CreatePetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.create(currentUser, request));
    }

    @GetMapping
    public ResponseEntity<List<PetResponse>> list(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(petService.list(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetResponse> get(@AuthenticationPrincipal CurrentUser currentUser,
                                           @PathVariable Long id) {
        return ResponseEntity.ok(petService.get(currentUser, id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PetResponse> update(@AuthenticationPrincipal CurrentUser currentUser,
                                              @PathVariable Long id,
                                              @Valid @RequestBody UpdatePetRequest request) {
        return ResponseEntity.ok(petService.update(currentUser, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser currentUser,
                                       @PathVariable Long id) {
        petService.delete(currentUser, id);
        return ResponseEntity.noContent().build();
    }
}
