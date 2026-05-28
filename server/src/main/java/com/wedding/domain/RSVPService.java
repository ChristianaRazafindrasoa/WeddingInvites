package com.wedding.domain;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.wedding.data.GuestRepository;
import com.wedding.data.RSVPRepository;
import com.wedding.dto.RSVPRequest;
import com.wedding.dto.RSVPResponse;
import com.wedding.model.Guest;
import com.wedding.model.RSVP;

@Service
public class RSVPService {
    private final GuestRepository guestRepo;
    private final RSVPRepository rsvpRepo;

    public RSVPService(GuestRepository guestRepo, RSVPRepository rsvpRepo) {
        this.guestRepo = guestRepo;
        this.rsvpRepo = rsvpRepo;
    }

    public RSVPResponse submit(RSVPRequest request) {
        Guest mainGuest = guestRepo
                .findByFullName(request.mainGuestName())
                .orElseThrow(() ->
                        new RuntimeException("Main guest not found"));
        RSVP rsvp = rsvpRepo
                .findByMainGuest(mainGuest)
                .orElseThrow(() ->
                        new RuntimeException("RSVP not found."));
        if (rsvp.getRespondedAt() != null) {
            throw new IllegalStateException("RSVP already submitted.");
        }
        Guest plusOne = null;
        if (request.plusOneName() != null && !request.plusOneName().isBlank()) {
            plusOne = guestRepo
                    .findByFullName(request.plusOneName())
                    .orElseThrow(() ->
                            new RuntimeException("Plus one not found"));
            plusOne.setAttending();
            plusOne = guestRepo.save(plusOne);
        }

        mainGuest.setAttending();
        mainGuest = guestRepo.save(mainGuest);

        rsvp.setRespondedAt(LocalDateTime.now());
        RSVP saved = rsvpRepo.save(rsvp);
        return new RSVPResponse(
                saved.getMainGuest().getFullName(),
                saved.getPlusOne() != null
                        ? saved.getPlusOne().getFullName()
                        : null,
                "Thank you for attending."
        );
    }
}