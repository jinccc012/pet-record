package com.harumi.petrecord.pet;

import com.harumi.petrecord.user.User;
import com.harumi.petrecord.user.UserRepository;
import com.harumi.petrecord.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
class PetRepositoryIT {

    @Autowired PetRepository petRepository;
    @Autowired UserRepository userRepository;

    @Test
    void migrationCreatesPetsTableAndJpaValidatesSchema() {
        User owner = persistOwner("alice", "alice@example.com");

        Pet saved = petRepository.save(Pet.builder()
                .ownerId(owner.getId())
                .name("Mochi")
                .species(PetSpecies.CAT)
                .gender(PetGender.FEMALE)
                .birthDate(LocalDate.of(2022, 3, 1))
                .color("grey")
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void softDeleteHidesPetFromQueries() {
        User owner = persistOwner("bob", "bob@example.com");
        Pet pet = petRepository.save(Pet.builder()
                .ownerId(owner.getId())
                .name("Ghost")
                .species(PetSpecies.DOG)
                .build());

        petRepository.delete(pet);

        assertThat(petRepository.findById(pet.getId())).isEmpty();
        assertThat(petRepository.findByIdAndOwnerId(pet.getId(), owner.getId())).isEmpty();
        assertThat(petRepository.findAllByOwnerIdOrderByIdAsc(owner.getId())).isEmpty();
    }

    @Test
    void findAllByOwnerScopesByOwner() {
        User alice = persistOwner("a1", "a1@example.com");
        User bob = persistOwner("b1", "b1@example.com");

        petRepository.save(Pet.builder().ownerId(alice.getId()).name("A1").species(PetSpecies.CAT).build());
        petRepository.save(Pet.builder().ownerId(alice.getId()).name("A2").species(PetSpecies.DOG).build());
        petRepository.save(Pet.builder().ownerId(bob.getId()).name("B1").species(PetSpecies.RABBIT).build());

        assertThat(petRepository.findAllByOwnerIdOrderByIdAsc(alice.getId())).hasSize(2);
        assertThat(petRepository.findAllByOwnerIdOrderByIdAsc(bob.getId())).hasSize(1);
    }

    private User persistOwner(String username, String email) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .passwordHash("hash")
                .role(UserRole.USER)
                .build());
    }
}
