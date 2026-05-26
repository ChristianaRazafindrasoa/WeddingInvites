package com.wedding.model;

import java.time.LocalDate;
import java.util.List;

public record WeddingInfo(String coupleNames, LocalDate weddingDate, String city, List<WeddingEvent> events) {
}