package com.wedding.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.dto.RSVPRequest;
import com.wedding.model.Guest;
import com.wedding.model.RSVP;

@RestController
@RequestMapping("api/rsvp")
@CrossOrigin(origins = "http://localhost:3000")
public class RSVPController {
    private final List<Guest> invitedGuests = List.of(
        new Guest("Test1 Test", "1234567890", true, false),
        new Guest("Test2 Test", null, false, false),
        new Guest("Test3 Test", "9876543210", false, false)
    );
    private final Set<Guest> confirmedGuests = new HashSet<>();
    private final List<RSVP> rsvps = new ArrayList<>();

    @PostMapping
    public RSVP submit(@RequestBody RSVPRequest request) {
        Guest mainGuest = invitedGuests.stream()
            .filter(g -> g.fullName().equalsIgnoreCase(request.mainGuestName()))
            .findFirst()
            .orElse(null);
        if (mainGuest == null) {
            return null;
        }
        confirmedGuests.add(mainGuest);

        Guest plusOne = null;
        if (request.plusOneName() != null && !request.plusOneName().isEmpty()) {
            plusOne = invitedGuests.stream()
                .filter(g -> g.fullName().equalsIgnoreCase(request.plusOneName()))
                .findFirst()
                .orElse(null);
            if (plusOne != null) {
                confirmedGuests.add(plusOne);
            }
        }
        
        RSVP rsvp = new RSVP(mainGuest, Optional.of(plusOne));
        rsvps.add(rsvp);
        System.out.println("New RSVP: " + rsvp);
        return rsvp;
    }
}