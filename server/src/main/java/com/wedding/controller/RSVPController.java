package com.wedding.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.wedding.domain.RSVPService;
import com.wedding.dto.RSVPRequest;
import com.wedding.dto.RSVPResponse;

@RestController
@RequestMapping("api/rsvp")
@CrossOrigin(origins = "http://localhost:3000")
public class RSVPController {

    private final RSVPService rsvpService;

    public RSVPController(RSVPService rsvpService) {
        this.rsvpService = rsvpService;
    }

    @PostMapping
    public ResponseEntity<RSVPResponse> submit(@RequestBody RSVPRequest request) {
        try {
            RSVPResponse response = rsvpService.submit(request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                new RSVPResponse(
                        null,
                        null,
                        e.getMessage()
                )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new RSVPResponse(
                        null,
                        null,
                        e.getMessage()
                )
            );
        }
    }
}