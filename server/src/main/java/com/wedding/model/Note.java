package com.wedding.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "note")
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Integer id;
    @Column(name = "wedding_id")
    private Integer weddingId = 1;
    @Column(name = "guest_name")
    private String guestName;
    @Column(name = "message")
    private String message;

    public Note() {}

    public Note(String guestName, String message) {
        this.guestName = guestName;
        this.message = message;
    }

    public Integer getId() {
        return id;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getMessage() {
        return message;
    }
}
