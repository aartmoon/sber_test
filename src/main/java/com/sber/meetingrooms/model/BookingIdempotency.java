package com.sber.meetingrooms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_idempotency")
public class BookingIdempotency {
    @Id
    @Column(name = "idempotency_key", length = 128)
    private String key;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

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

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    protected BookingIdempotency() {
    }

    public BookingIdempotency(String key, String fingerprint, Booking booking,
                              OffsetDateTime expiresAt) {
        this.key = key;
        this.fingerprint = fingerprint;
        this.bookingId = booking.id();
        this.roomId = booking.roomId();
        this.employeeEmail = booking.employeeEmail();
        this.startsAt = booking.startsAt();
        this.endsAt = booking.endsAt();
        this.createdAt = booking.createdAt();
        this.expiresAt = expiresAt;
    }

    public String fingerprint() {
        return fingerprint;
    }

    public Booking booking() {
        return new Booking(bookingId, roomId, employeeEmail, startsAt, endsAt, createdAt);
    }

    public void expireAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
