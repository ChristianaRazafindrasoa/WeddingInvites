package com.wedding;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;

import com.wedding.data.GuestRepository;
import com.wedding.data.RSVPRepository;
import com.wedding.domain.RSVPService;
import com.wedding.dto.RSVPRequest;
import com.wedding.exception.WeddingException;
import com.wedding.model.Guest;
import com.wedding.model.RSVP;

@DataJpaTest
public class RSVPServiceTest {
    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private RSVPRepository rsvpRepo;    
    @Autowired
    private GuestRepository guestRepo;
    private RSVPService service;
    private Guest guestOne;
    private Guest guestTwo;
    private Guest guestThree;

    @BeforeEach
    void setUp() {
        service = new RSVPService(guestRepo, rsvpRepo, txManager);
        guestOne = guestRepo.save(new Guest(1, "John Doe", "1112223333", true, false));
        guestTwo = guestRepo.save(new Guest(2, "Jane Doe", "4445556666", false, false));
        guestThree = guestRepo.save(new Guest(3, "Janette Doe", "4445556666", false, false));
        rsvpRepo.save(new RSVP("test1-token", guestOne, guestTwo, null, false));
        rsvpRepo.save(new RSVP("test2-token", guestTwo, null, null, false));
        rsvpRepo.save(new RSVP("test3-token", guestThree, null, null, false));
    }

    @Test
    void submitRollsBackWhenPlusOneNotFound() {
        RSVPRequest request = new RSVPRequest("test1-token", guestOne.getFullName(), "not-a-guest", true);
        assertThrows(WeddingException.class, () -> service.submit(request));
        Guest updatedGuest = guestRepo.findById(guestOne.getId()).orElseThrow();
        RSVP updatedRSVP = rsvpRepo.findByToken("test1-token").orElseThrow();
        assertFalse(updatedGuest.isAttending());
        assertFalse(updatedRSVP.isAccepted());
        assertNull(updatedRSVP.getRespondedAt());
    }

    @Test
    void submitSavesGuestAndRSVPWhenAccepted() {
        RSVPRequest request = new RSVPRequest("test2-token", guestTwo.getFullName(), null, true);
        service.submit(request);
        Guest updatedGuest = guestRepo.findById(guestTwo.getId()).orElseThrow();
        RSVP updatedRSVP = rsvpRepo.findByToken("test2-token").orElseThrow();
        assertTrue(updatedGuest.isAttending());
        assertTrue(updatedRSVP.isAccepted());
        assertNotNull(updatedRSVP.getRespondedAt());
    }

    @Test
    void submitRollsBackWhenRsvpSaveFails() {
        RSVPRepository spyRsvpRepo = mock(RSVPRepository.class);
        when(spyRsvpRepo.findByToken(any())).thenAnswer(inv -> rsvpRepo.findByToken(inv.getArgument(0)));
        doThrow(new RuntimeException("simulated failure")).when(spyRsvpRepo).save(any());
        RSVPService spyRsvpService = new RSVPService(guestRepo, spyRsvpRepo, txManager);
        RSVPRequest request = new RSVPRequest("test3-token", guestThree.getFullName(), null, true);
        assertThrows(RuntimeException.class, () -> spyRsvpService.submit(request));
        Guest updatedGuest = guestRepo.findById(guestThree.getId()).orElseThrow();
        RSVP updatedRSVP = rsvpRepo.findByToken("test3-token").orElseThrow();
        assertFalse(updatedGuest.isAttending());
        assertFalse(updatedRSVP.isAccepted());
        assertNull(updatedRSVP.getRespondedAt());
    }
}
