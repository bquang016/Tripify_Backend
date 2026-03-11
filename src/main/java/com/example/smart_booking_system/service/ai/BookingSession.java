package com.example.smart_booking_system.service.ai;

import java.time.Instant;

public class BookingSession {

    private boolean active;
    private String city;
    private Integer capacity;

    private Instant updatedAt;

    public BookingSession() {
        this.active = false;
        this.city = null;
        this.capacity = null;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        touch();
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
        touch();
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
        touch();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public void reset() {
        this.active = false;
        this.city = null;
        this.capacity = null;
        touch();
    }
}
