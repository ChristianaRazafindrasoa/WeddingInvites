package com.wedding.dto;

public record RSVPRequest(String token, String mainGuestName, String plusOneName, boolean isAccepted) {
}
