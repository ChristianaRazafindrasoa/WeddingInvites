package com.wedding.dto;

import java.util.Optional;

public record RSVPResponse(String mainGuestName, String plusOneName, boolean hasPlusOne, boolean isSubmitted, Optional<String> message) {
}
