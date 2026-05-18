package com.harumi.petrecord.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
class UserRepositoryIT {

    @Autowired UserRepository userRepository;

    @Test
    void migrationCreatesUsersTableAndJpaValidatesSchema() {
        User saved = userRepository.save(User.builder()
                .username("harumi")
                .email("harumi@example.com")
                .passwordHash("hash")
                .role(UserRole.USER)
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void softDeleteHidesEntityFromQueries() {
        User user = userRepository.save(User.builder()
                .username("ghost")
                .email("ghost@example.com")
                .passwordHash("hash")
                .role(UserRole.USER)
                .build());

        userRepository.delete(user);

        assertThat(userRepository.findByEmail("ghost@example.com")).isEmpty();
        assertThat(userRepository.existsByEmail("ghost@example.com")).isFalse();
    }

    @Test
    void findByEmailReturnsUser() {
        userRepository.save(User.builder()
                .username("findme")
                .email("findme@example.com")
                .passwordHash("hash")
                .role(UserRole.USER)
                .build());

        assertThat(userRepository.findByEmail("findme@example.com")).isPresent();
        assertThat(userRepository.existsByUsername("findme")).isTrue();
    }
}
