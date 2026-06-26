package com.wedding.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.domain.AdminService;
import com.wedding.exception.WeddingException;
import com.wedding.model.Guest;
import com.wedding.model.RSVP;

@RestController
@RequestMapping("admin")
@CrossOrigin(origins = {"http://localhost:3000", "http://3.80.113.81:8080"})
public class AdminController {
    private final AdminService adminService;
    private final String adminPassword;

    public AdminController(AdminService adminService, @Value("${admin.password}") String adminPassword) {
        this.adminService = adminService;
        this.adminPassword = adminPassword;
    }

    @GetMapping("/guests")
    public ResponseEntity<List<Guest>> getAllGuests (
        @RequestHeader(value = "Authorization", required = false) String auth) {
        if (!isAuthorized(auth)) {
            throw new WeddingException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return ResponseEntity.ok(adminService.getInvitedGuests());
    }

    @GetMapping("/rsvps")
    public ResponseEntity<List<RSVP>> getAllRsvps (
        @RequestHeader(value = "Authorization", required = false) String auth) {
        if (!isAuthorized(auth)) {
            throw new WeddingException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return ResponseEntity.ok(adminService.getCreatedRSVPs());
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        if (!adminPassword.equals(body.get("password"))) {
            throw new WeddingException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return ResponseEntity.ok(Map.of("token", adminService.generateToken()));
    }

    private boolean isAuthorized(String auth) {
        if (auth == null) {
            return false;
        }
        String token = auth.substring(7);
        return auth.startsWith("Bearer ") && adminService.validateToken(token);
    }
}
