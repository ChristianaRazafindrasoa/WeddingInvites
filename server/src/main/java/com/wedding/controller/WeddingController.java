package com.wedding.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.model.WeddingEvent;
import com.wedding.model.WeddingInfo;

@RestController
@RequestMapping("api")
@CrossOrigin(origins = "http://localhost:3000")
public class WeddingController {

    @GetMapping("/info")
    public WeddingInfo getWeddingInfo() {
        List<WeddingEvent> events = List.of(
                new WeddingEvent(
                        "Ceremony",
                        "Amazing Church",
                        "To Be Determined, Tampa",
                        LocalDateTime.of(2026, 12, 12, 7, 30)
                ),
                new WeddingEvent(
                        "Dinner",
                        "Awesome Restaurant",
                        "To Be Determined, Tampa",
                        LocalDateTime.of(2026, 12, 12, 8, 15)
                )
        );
        return new WeddingInfo(
                "Nicholas & Christiana",
                LocalDate.of(2026, 12, 12),
                "Tampa, Florida",
                events
        );
    }
}
