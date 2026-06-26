package com.wedding.dto;

import java.util.Optional;

public record RSVPResponse(String mainGuestName, String plusOneName, boolean hasPlusOne, Optional<String> message) {
}
