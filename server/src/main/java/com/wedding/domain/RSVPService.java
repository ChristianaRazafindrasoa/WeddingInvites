package com.wedding.domain;

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
            .orElseThrow(() -> new RuntimeException("Guest not found"));
        mainGuest.setConfirmed();
        guestRepo.save(mainGuest);

        Guest plusOne = null;
        if (request.plusOneName() != null && !request.plusOneName().isBlank()) {
            plusOne = guestRepo.findByFullName(request.plusOneName())
                    .orElse(null);
            if (plusOne != null) {
                plusOne.setConfirmed();
                guestRepo.save(plusOne);
            }
        }

        RSVP rsvp = new RSVP(mainGuest, plusOne);
        RSVP saved = rsvpRepo.save(rsvp);
        System.out.println(rsvp);
        return new RSVPResponse(
                saved.getMainGuest().getFullName(),
                saved.getPlusOne() != null ? saved.getPlusOne().getFullName() : null,
                saved.getPlusOne() != null,
                true
        );
    }
}