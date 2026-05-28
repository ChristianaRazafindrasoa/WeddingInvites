package com.wedding.data;

import org.springframework.data.jpa.repository.JpaRepository;
import com.wedding.model.RSVP;

public interface RSVPRepository extends JpaRepository<RSVP, Integer> {
}