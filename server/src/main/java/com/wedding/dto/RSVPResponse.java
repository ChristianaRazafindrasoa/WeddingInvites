package com.wedding.dto;

public record RSVPResponse(String mainGuestName, String plusOneName, boolean hasPlusOne, boolean isConfirmed) {
}
