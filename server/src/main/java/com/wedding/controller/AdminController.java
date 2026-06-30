package com.wedding.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.wedding.domain.AdminService;
import com.wedding.exception.WeddingException;
import com.wedding.model.Guest;
import com.wedding.model.RSVP;

@RestController
@CrossOrigin(origins = "${app.cors.origins}")
public class AdminController implements ErrorController {
    private final AdminService adminService;
    private final String adminPassword;

    public AdminController(
            AdminService adminService,
            @Value("${admin.password}") String adminPassword) {
        this.adminService = adminService;
        this.adminPassword = adminPassword;
    }

    @GetMapping(value = {"/", "/{path:[^\\.]*}"})
    public void index(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.getRequestDispatcher("/index.html").forward(request, response);
    }

    @RequestMapping("/error")
    public void handleError(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.getRequestDispatcher("/index.html").forward(request, response);
    }

    @GetMapping("/admin/guests")
    public ResponseEntity<List<Guest>> getAllGuests(
        @RequestHeader(value = "Authorization", required = false) String auth) {
        if (!isAuthorized(auth)) {
            throw new WeddingException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return ResponseEntity.ok(adminService.getInvitedGuests());
    }

    @GetMapping("/admin/rsvps")
    public ResponseEntity<List<RSVP>> getAllRsvps(
        @RequestHeader(value = "Authorization", required = false) String auth) {
        if (!isAuthorized(auth)) {
            throw new WeddingException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return ResponseEntity.ok(adminService.getCreatedRSVPs());
    }

    @PostMapping("/admin/login")
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
        String bearer = "Bearer ";
        String token = auth.substring(bearer.length());
        return auth.startsWith(bearer) && adminService.validateToken(token);
    }
}