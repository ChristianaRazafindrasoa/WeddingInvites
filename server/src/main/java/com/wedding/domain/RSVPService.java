package com.wedding.domain;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.wedding.data.GuestRepository;
import com.wedding.data.RSVPRepository;
import com.wedding.dto.RSVPRequest;
import com.wedding.dto.RSVPResponse;
import com.wedding.exception.WeddingException;
import com.wedding.model.Guest;
import com.wedding.model.RSVP;

@Service
public class RSVPService {
    private final GuestRepository guestRepo;
    private final RSVPRepository rsvpRepo;
    private final TransactionTemplate transaction;

    public RSVPService(GuestRepository guestRepo, RSVPRepository rsvpRepo, PlatformTransactionManager txManager) {
        this.guestRepo = guestRepo;
        this.rsvpRepo = rsvpRepo;
        this.transaction = new TransactionTemplate(txManager);
    }

    public RSVPResponse findByToken(String token) {
        RSVP rsvp = rsvpRepo.findByToken(token)
            .orElseThrow(() -> new WeddingException(HttpStatus.NOT_FOUND, "RSVP not found."));
        return new RSVPResponse(
            rsvp.getMainGuest().getFullName(),
            rsvp.getPlusOne() != null ? rsvp.getPlusOne().getFullName() : null,
            rsvp.getMainGuest().hasPlusOne(),
            Optional.empty()
        );
    }

    public RSVPResponse submit(RSVPRequest request) {
        RSVP rsvp = rsvpRepo.findByToken(request.token())
            .orElseThrow(() -> new WeddingException(HttpStatus.NOT_FOUND, "RSVP not found."));
        if (rsvp.getRespondedAt() != null) {
            throw new WeddingException(HttpStatus.CONFLICT, "RSVP already submitted.");
        }
        Guest mainGuest = rsvp.getMainGuest();
        if (!mainGuest.getFullName().equals(request.mainGuestName())) {
            throw new WeddingException(HttpStatus.BAD_REQUEST, "Guest does not match token.");
        }
        RSVP saved = persist(rsvp, mainGuest, request);
        String message = request.isAccepted() ?
            "Thank you for attending. 🤍" :
            "Thank you, we'll miss you. 🤍";
        return new RSVPResponse(
            saved.getMainGuest().getFullName(),
            saved.getPlusOne() != null ? saved.getPlusOne().getFullName() : null,
            saved.getMainGuest().hasPlusOne(),
            Optional.of(message));
    }

    private RSVP persist(RSVP rsvp, Guest mainGuest, RSVPRequest request) {
        return transaction.execute(status -> {
            try {
                if (request.plusOneName() != null && !request.plusOneName().isBlank()) {
                    Guest plusOne = guestRepo.findByFullName(request.plusOneName())
                        .orElseThrow(() -> new WeddingException(
                            HttpStatus.NOT_FOUND, "Plus one not found."));
                    plusOne.setAttending(request.isAccepted());
                    guestRepo.save(plusOne);
                }
                mainGuest.setAttending(request.isAccepted());
                guestRepo.save(mainGuest);
                rsvp.setRespondedAt(LocalDateTime.now());
                rsvp.setAccepted(request.isAccepted());
                return rsvpRepo.save(rsvp);
            } catch (RuntimeException e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }
}