package com.wedding.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "rsvp")
public class RSVP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @OneToOne
    private Guest mainGuest;
    @OneToOne
    private Guest plusOne;

    public RSVP() {}

    public RSVP(Guest mainGuest, Guest plusOne) {
        this.mainGuest = mainGuest;
        this.plusOne = plusOne;
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
}