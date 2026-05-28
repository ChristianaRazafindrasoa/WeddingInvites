package com.wedding.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.model.Guest;
import com.wedding.model.RSVP;

public interface RSVPRepository extends JpaRepository<RSVP, Integer> {
    Optional<RSVP> findByMainGuest(Guest mainGuest);
}