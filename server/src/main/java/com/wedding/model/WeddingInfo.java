package com.wedding.model;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "wedding_info")
public class WeddingInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wedding_id")
    private Integer id;
    @Column(name = "groom_name")
    private String groomName;
    @Column(name = "bride_name")
    private String brideName;
    @Column(name = "date")
    private LocalDate weddingDate;
    @Column(name = "city")
    private String city;
    @OneToMany
    @JoinColumn(name = "wedding_id")    
    private List<WeddingEvent> events;

    public WeddingInfo() {
    }

    public WeddingInfo(String groomName, String brideName, LocalDate weddingDate, String city, List<WeddingEvent> events) {
        this.groomName = groomName;
        this.brideName = brideName;
        this.weddingDate = weddingDate;
        this.city = city;
        this.events = events;
    }

    public Integer getId() {
        return id;
    }

    public String getGroomName() {
        return groomName;
    }

    public String getBrideName() {
        return brideName;
    }

    public LocalDate getWeddingDate() {
        return weddingDate;
    }

    public String getCity() {
        return city;
    }
    
    public List<WeddingEvent> getEvents() {
        return events;
    }
}