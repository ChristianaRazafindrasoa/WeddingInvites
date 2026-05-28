package com.wedding.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "rsvp")
public class RSVP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rsvp_id")
    private Integer id;
    @OneToOne
    @JoinColumn(name = "main_guest_id")
    private Guest mainGuest;
    @OneToOne
    @JoinColumn(name = "plus_one_id")
    private Guest plusOne;
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    @Column(name = "is_accepted")
    private boolean isAccepted;

    public RSVP() {}

    public RSVP(Guest mainGuest, Guest plusOne, LocalDateTime respondedAt, boolean isAccepted) {
        this.mainGuest = mainGuest;
        this.plusOne = plusOne;
        this.respondedAt = respondedAt;
        this.isAccepted = isAccepted;
    }

    public int getId() {
        return id;
    }

    public Guest getMainGuest() {
        return mainGuest;
    }

    public Guest getPlusOne() {
        return plusOne;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime timestamp) {
        respondedAt = timestamp;
    }

    public boolean isAccepted() {
        return isAccepted;
    }

    public void setAccepted(boolean isAccepted) {
        this.isAccepted = isAccepted;
    }
}