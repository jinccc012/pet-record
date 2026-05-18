package com.harumi.petrecord.pet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    Optional<Pet> findByIdAndOwnerId(Long id, Long ownerId);

    List<Pet> findAllByOwnerIdOrderByIdAsc(Long ownerId);
}
