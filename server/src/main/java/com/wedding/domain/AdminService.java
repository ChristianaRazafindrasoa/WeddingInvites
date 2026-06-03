package com.wedding.domain;

import java.util.List;

import org.springframework.stereotype.Service;

import com.wedding.data.GuestRepository;
import com.wedding.data.RSVPRepository;
import com.wedding.model.Guest;
import com.wedding.model.RSVP;

@Service
public class AdminService {
    private final GuestRepository guestRepo;
    private final RSVPRepository rsvpRepo;

    public AdminService(GuestRepository guestRepo, RSVPRepository rsvpRepo) {
        this.guestRepo = guestRepo;
        this.rsvpRepo = rsvpRepo;
    }

    public List<Guest> getInvitedGuests() {
        return guestRepo.findAll();
    }

    public List<RSVP> getCreatedRSVPs() {
        return rsvpRepo.findAll();
    }
}
