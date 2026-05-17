package com.harumi.petrecord.pet.dto;

import com.harumi.petrecord.pet.Pet;
import com.harumi.petrecord.pet.PetGender;
import com.harumi.petrecord.pet.PetSpecies;

import java.time.Instant;
import java.time.LocalDate;

public record PetResponse(
        Long id,
        String name,
        PetSpecies species,
        String breed,
        PetGender gender,
        LocalDate birthDate,
        String color,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static PetResponse from(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getGender(),
                pet.getBirthDate(),
                pet.getColor(),
                pet.getNotes(),
                pet.getCreatedAt(),
                pet.getUpdatedAt()
        );
    }
}
