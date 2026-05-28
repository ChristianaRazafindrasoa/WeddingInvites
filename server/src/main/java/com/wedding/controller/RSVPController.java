package com.wedding.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.wedding.domain.RSVPService;
import com.wedding.dto.RSVPRequest;
import com.wedding.dto.RSVPResponse;
import com.wedding.model.Guest;

@RestController
@RequestMapping("api/rsvp")
@CrossOrigin(origins = "http://localhost:3000")
public class RSVPController {

    private final RSVPService rsvpService;

    public RSVPController(RSVPService rsvpService) {
        this.rsvpService = rsvpService;
    }

    @PostMapping
    public RSVPResponse submit(@RequestBody RSVPRequest request) {
        return rsvpService.submit(request);
    }
}