package com.wedding.model;

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
    private int id;
    private String fullName;
    private String phoneNumber;
    private boolean hasPlusOne;
    private boolean isConfirmed;

    public Guest() {}

    public Guest(int id, String fullName, String phoneNumber, boolean hasPlusOne, boolean isConfirmed) {
        this.id = id;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.hasPlusOne = hasPlusOne;
        this.isConfirmed = isConfirmed;
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

    public boolean isHasPlusOne() {
        return hasPlusOne;
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public void setConfirmed() {
        isConfirmed = true;
    }

    public void undoConfirmed() {
        isConfirmed = false;
    }
}