package com.wedding.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "wedding_event")
public class WeddingEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Integer id;
    @Column(name = "name")
    private String name;
    @Column(name = "location")
    private String location;
    @Column(name = "address")
    private String address;
    @Column(name = "start_time")
    private LocalDateTime startTime;

    public WeddingEvent() {}

    public WeddingEvent(String name, String location, String address, LocalDateTime startTime) {
        this.name = name;
        this.location = location;
        this.address = address;
        this.startTime = startTime;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getAddress() {
        return address;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }
}

