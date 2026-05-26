package com.wedding.model;

import java.util.Optional;

public record RSVP(Guest mainGuest, Optional<Guest> plusOne) {
    @Override
    public String toString() {
        return mainGuest.fullName() + " & " +
               plusOne.map(Guest::fullName)
                      .orElse("No plus one");
    }
}