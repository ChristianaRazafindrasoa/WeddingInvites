package com.wedding.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.data.WeddingInfoRepository;
import com.wedding.model.WeddingInfo;

@RestController
@RequestMapping("api")
@CrossOrigin(origins = "http://localhost:3000")
public class WeddingController {
    private WeddingInfoRepository infoRepo;

    public WeddingController(WeddingInfoRepository infoRepo) {
        this.infoRepo = infoRepo;
    }

    @GetMapping("/info")
    public WeddingInfo getWeddingInfo() {
        return infoRepo.findAll().stream().findFirst().orElseThrow();
    }
}

