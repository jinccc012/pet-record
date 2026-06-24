package com.harumi.petrecord.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    /**
     * Current token version for an existing (non-deleted) user. Empty when the user is missing or
     * soft-deleted, which the JWT filter treats as an invalid token.
     */
    @Query("select u.tokenVersion from User u where u.id = :id")
    Optional<Integer> findTokenVersionById(@Param("id") Long id);

    @Modifying
    @Query("update User u set u.tokenVersion = u.tokenVersion + 1 where u.id = :id")
    int incrementTokenVersion(@Param("id") Long id);
}
