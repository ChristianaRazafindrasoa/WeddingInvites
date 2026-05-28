package com.wedding.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.wedding.model.Guest;

public interface GuestRepository extends JpaRepository<Guest, Integer> {
    Optional<Guest> findByFullName(String fullName);
}