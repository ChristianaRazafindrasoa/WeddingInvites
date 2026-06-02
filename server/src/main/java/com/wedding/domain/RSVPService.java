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

    public RSVPResponse findByToken(String token) {
        RSVP rsvp = rsvpRepo.findByToken(token)
            .orElseThrow(() -> new RuntimeException("RSVP not found."));

        return new RSVPResponse(
            rsvp.getMainGuest().getFullName(),
            rsvp.getPlusOne() != null ? rsvp.getPlusOne().getFullName() : null,
            rsvp.getMainGuest().hasPlusOne(),
            null
        );
    }

    public RSVPResponse submit(RSVPRequest request) {
        RSVP rsvp = rsvpRepo.findByToken(request.token())
            .orElseThrow(() -> new RuntimeException("RSVP token not found."));

        if (rsvp.getRespondedAt() != null) {
            throw new IllegalStateException("RSVP already submitted.");
        }

        Guest mainGuest = rsvp.getMainGuest();
        if (!mainGuest.getFullName().equals(request.mainGuestName())) {
            throw new RuntimeException("Guest does not match token.");
        }

        Guest plusOne = null;
        if (request.plusOneName() != null && !request.plusOneName().isBlank()) {
            plusOne = guestRepo.findByFullName(request.plusOneName())
                .orElseThrow(() -> new RuntimeException("Plus one not found"));
            plusOne.setAttending(request.isAccepted());
            plusOne = guestRepo.save(plusOne);
        }

        mainGuest.setAttending(request.isAccepted());
        guestRepo.save(mainGuest);

        rsvp.setRespondedAt(LocalDateTime.now());
        rsvp.setAccepted(request.isAccepted());
        RSVP saved = rsvpRepo.save(rsvp);

        return new RSVPResponse(
            saved.getMainGuest().getFullName(),
            saved.getPlusOne() != null ? saved.getPlusOne().getFullName() : null,
            saved.getMainGuest().hasPlusOne(),
            request.isAccepted() ? "Thank you for attending." : "Thank you, we'll miss you."
        );
    }
}