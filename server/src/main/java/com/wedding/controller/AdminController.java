package com.wedding.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.domain.AdminService;
import com.wedding.model.Guest;
import com.wedding.model.RSVP;

@RestController
@RequestMapping("api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/guests")
    public List<Guest> getAllGuests() {
        return adminService.getInvitedGuests();
    }

    @GetMapping("/rsvps")
    public List<RSVP> getAllRsvps() {
        return adminService.getCreatedRSVPs();
    }
}
