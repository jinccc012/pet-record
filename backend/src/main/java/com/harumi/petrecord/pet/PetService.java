package com.harumi.petrecord.pet;

import com.harumi.petrecord.common.exception.ResourceNotFoundException;
import com.harumi.petrecord.pet.dto.CreatePetRequest;
import com.harumi.petrecord.pet.dto.PetResponse;
import com.harumi.petrecord.pet.dto.UpdatePetRequest;
import com.harumi.petrecord.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PetService {

    private static final Logger log = LoggerFactory.getLogger(PetService.class);

    private final PetRepository petRepository;

    public PetService(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Transactional
    public PetResponse create(CurrentUser currentUser, CreatePetRequest request) {
        Pet pet = Pet.builder()
                .ownerId(currentUser.id())
                .name(request.name())
                .species(request.species())
                .breed(request.breed())
                .gender(request.gender())
                .birthDate(request.birthDate())
                .color(request.color())
                .notes(request.notes())
                .build();
        Pet saved = petRepository.save(pet);
        log.info("Created pet id={} ownerId={}", saved.getId(), saved.getOwnerId());
        return PetResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PetResponse> list(CurrentUser currentUser) {
        return petRepository.findAllByOwnerIdOrderByIdAsc(currentUser.id()).stream()
                .map(PetResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PetResponse get(CurrentUser currentUser, Long petId) {
        return PetResponse.from(loadOwned(currentUser, petId));
    }

    @Transactional
    public PetResponse update(CurrentUser currentUser, Long petId, UpdatePetRequest request) {
        Pet pet = loadOwned(currentUser, petId);
        if (request.name() != null) {
            pet.setName(request.name());
        }
        if (request.species() != null) {
            pet.setSpecies(request.species());
        }
        if (request.breed() != null) {
            pet.setBreed(request.breed());
        }
        if (request.gender() != null) {
            pet.setGender(request.gender());
        }
        if (request.birthDate() != null) {
            pet.setBirthDate(request.birthDate());
        }
        if (request.color() != null) {
            pet.setColor(request.color());
        }
        if (request.notes() != null) {
            pet.setNotes(request.notes());
        }
        log.info("Updated pet id={} ownerId={}", pet.getId(), pet.getOwnerId());
        return PetResponse.from(pet);
    }

    @Transactional
    public void delete(CurrentUser currentUser, Long petId) {
        Pet pet = loadOwned(currentUser, petId);
        petRepository.delete(pet);
        log.info("Soft-deleted pet id={} ownerId={}", pet.getId(), pet.getOwnerId());
    }

    private Pet loadOwned(CurrentUser currentUser, Long petId) {
        return petRepository.findByIdAndOwnerId(petId, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
    }
}
