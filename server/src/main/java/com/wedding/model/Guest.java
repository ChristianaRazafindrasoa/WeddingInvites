package com.wedding.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "guest")
public class Guest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guest_id")
    private Integer id;
    @Column(name = "wedding_id")
    private Integer weddingId = 1;
    @Column(name = "name")
    private String fullName;
    @Column(name = "phone")
    private String phoneNumber;
    @Column(name = "has_plus_one")
    private boolean hasPlusOne;
    @Column(name = "is_attending")
    private boolean isAttending;

    public Guest() {}

    public Guest(int id, String fullName, String phoneNumber, boolean hasPlusOne, boolean isAttending) {
        this.id = id;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.hasPlusOne = hasPlusOne;
        this.isAttending = isAttending;
    }

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean hasPlusOne() {
        return hasPlusOne;
    }

    public boolean isAttending() {
        return isAttending;
    }

    public void setAttending(boolean isAttending) {
        this.isAttending = isAttending;
    }
}