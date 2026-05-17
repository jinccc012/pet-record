package com.harumi.petrecord.pet.dto;

import com.harumi.petrecord.pet.PetGender;
import com.harumi.petrecord.pet.PetSpecies;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePetRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull PetSpecies species,
        @Size(max = 100) String breed,
        PetGender gender,
        @PastOrPresent LocalDate birthDate,
        @Size(max = 50) String color,
        @Size(max = 2000) String notes
) {
}
