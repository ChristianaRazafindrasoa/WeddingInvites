package com.wedding.model;

import java.time.LocalDateTime;

public record WeddingEvent(String name, String location, String address, LocalDateTime startTime) {
}