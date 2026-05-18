package com.harumi.petrecord.pet;

import com.harumi.petrecord.common.exception.ResourceNotFoundException;
import com.harumi.petrecord.pet.dto.CreatePetRequest;
import com.harumi.petrecord.pet.dto.PetResponse;
import com.harumi.petrecord.pet.dto.UpdatePetRequest;
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
class PetServiceTest {

    @Mock PetRepository petRepository;

    @InjectMocks PetService petService;

    private static final CurrentUser OWNER = new CurrentUser(1L, "alice", UserRole.USER);
    private static final CurrentUser STRANGER = new CurrentUser(2L, "bob", UserRole.USER);

    @Test
    void createAssignsOwnerAndPersists() {
        CreatePetRequest req = new CreatePetRequest(
                "Mochi", PetSpecies.CAT, "British Shorthair", PetGender.FEMALE,
                LocalDate.of(2022, 3, 1), "grey", "shy");
        when(petRepository.save(any(Pet.class))).thenAnswer(inv -> {
            Pet p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        PetResponse response = petService.create(OWNER, req);

        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository).save(captor.capture());
        Pet saved = captor.getValue();
        assertThat(saved.getOwnerId()).isEqualTo(1L);
        assertThat(saved.getName()).isEqualTo("Mochi");
        assertThat(saved.getSpecies()).isEqualTo(PetSpecies.CAT);
        assertThat(saved.getGender()).isEqualTo(PetGender.FEMALE);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Mochi");
    }

    @Test
    void listReturnsOnlyOwnersPets() {
        Pet pet = samplePet(11L, OWNER.id());
        when(petRepository.findAllByOwnerIdOrderByIdAsc(OWNER.id())).thenReturn(List.of(pet));

        List<PetResponse> result = petService.list(OWNER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(11L);
    }

    @Test
    void getReturnsOwnedPet() {
        Pet pet = samplePet(12L, OWNER.id());
        when(petRepository.findByIdAndOwnerId(12L, OWNER.id())).thenReturn(Optional.of(pet));

        PetResponse response = petService.get(OWNER, 12L);

        assertThat(response.id()).isEqualTo(12L);
    }

    @Test
    void getThrowsWhenPetBelongsToAnotherUser() {
        when(petRepository.findByIdAndOwnerId(12L, STRANGER.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.get(STRANGER, 12L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getThrowsWhenPetMissing() {
        when(petRepository.findByIdAndOwnerId(99L, OWNER.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.get(OWNER, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppliesOnlyNonNullFields() {
        Pet pet = samplePet(20L, OWNER.id());
        pet.setName("Mochi");
        pet.setNotes("original notes");
        pet.setColor("grey");
        when(petRepository.findByIdAndOwnerId(20L, OWNER.id())).thenReturn(Optional.of(pet));

        UpdatePetRequest req = new UpdatePetRequest(
                "Mochi II", null, null, null, null, null, null);
        PetResponse response = petService.update(OWNER, 20L, req);

        assertThat(response.name()).isEqualTo("Mochi II");
        assertThat(response.notes()).isEqualTo("original notes");
        assertThat(response.color()).isEqualTo("grey");
    }

    @Test
    void updateRejectsForeignPet() {
        when(petRepository.findByIdAndOwnerId(20L, STRANGER.id())).thenReturn(Optional.empty());

        UpdatePetRequest req = new UpdatePetRequest(
                "Hacked", null, null, null, null, null, null);
        assertThatThrownBy(() -> petService.update(STRANGER, 20L, req))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(petRepository, never()).save(any());
    }

    @Test
    void deleteSoftDeletesOwnedPet() {
        Pet pet = samplePet(30L, OWNER.id());
        when(petRepository.findByIdAndOwnerId(30L, OWNER.id())).thenReturn(Optional.of(pet));

        petService.delete(OWNER, 30L);

        verify(petRepository).delete(pet);
    }

    @Test
    void deleteRejectsForeignPet() {
        when(petRepository.findByIdAndOwnerId(30L, STRANGER.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.delete(STRANGER, 30L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(petRepository, never()).delete(any());
    }

    private Pet samplePet(Long id, Long ownerId) {
        return Pet.builder()
                .id(id)
                .ownerId(ownerId)
                .name("Mochi")
                .species(PetSpecies.CAT)
                .gender(PetGender.FEMALE)
                .build();
    }
}
