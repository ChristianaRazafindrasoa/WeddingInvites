package com.wedding.domain;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wedding.data.GuestRepository;
import com.wedding.data.RSVPRepository;
import com.wedding.model.Guest;
import com.wedding.model.RSVP;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class AdminService {
    private final GuestRepository guestRepo;
    private final RSVPRepository rsvpRepo;
    private final String secret;
    private final int expiryHours;

    public AdminService(
            GuestRepository guestRepo, 
            RSVPRepository rsvpRepo, 
            @Value("${admin.jwt.secret}") String secret,
            @Value("${admin.jwt.expiry-hours}") int expiryHours) {
        this.guestRepo = guestRepo;
        this.rsvpRepo = rsvpRepo;
        this.secret = secret;
        this.expiryHours = expiryHours;
    }

    public List<Guest> getInvitedGuests() {
        return guestRepo.findAll();
    }

    public List<RSVP> getCreatedRSVPs() {
        return rsvpRepo.findAll();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate() {
        return Jwts.builder()
            .subject("admin")
            .expiration(new Date(System.currentTimeMillis() + expiryHours * 3_600_000L))
            .signWith(key())
            .compact();
    }

    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
