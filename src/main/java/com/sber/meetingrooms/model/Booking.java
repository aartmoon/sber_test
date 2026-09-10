package com.sber.meetingrooms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    private UUID id;

    @Column(name = "room_id", nullable = false, length = 50)
    private String roomId;

    @Column(name = "employee_email", nullable = false, length = 254)
    private String employeeEmail;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    protected Booking() {
    }

    public Booking(UUID id, String roomId, String employeeEmail,
                   OffsetDateTime startsAt, OffsetDateTime endsAt, OffsetDateTime createdAt) {
        this.id = id;
        this.roomId = roomId;
        this.employeeEmail = employeeEmail;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public String roomId() {
        return roomId;
    }

    public String employeeEmail() {
        return employeeEmail;
    }

    public OffsetDateTime startsAt() {
        return startsAt;
    }

    public OffsetDateTime endsAt() {
        return endsAt;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public OffsetDateTime cancelledAt() {
        return cancelledAt;
    }
}
