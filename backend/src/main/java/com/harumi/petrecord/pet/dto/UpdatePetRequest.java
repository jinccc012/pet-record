package com.harumi.petrecord.pet.dto;

import com.harumi.petrecord.pet.PetGender;
import com.harumi.petrecord.pet.PetSpecies;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdatePetRequest(
        @Size(min = 1, max = 100) String name,
        PetSpecies species,
        @Size(max = 100) String breed,
        PetGender gender,
        @PastOrPresent LocalDate birthDate,
        @Size(max = 50) String color,
        @Size(max = 2000) String notes
) {
}
