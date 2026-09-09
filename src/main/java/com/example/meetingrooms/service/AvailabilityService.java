package com.example.meetingrooms.service;

import com.example.meetingrooms.dto.Availability;
import com.example.meetingrooms.repository.BookingRepository;
import com.example.meetingrooms.repository.RoomRepository;
import com.example.meetingrooms.validation.BookingIntervalValidator;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvailabilityService {
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final BookingIntervalValidator intervalValidator;

    public AvailabilityService(BookingRepository bookings, RoomRepository rooms,
                               BookingIntervalValidator intervalValidator) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.intervalValidator = intervalValidator;
    }

    public Availability availability(String roomId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (!rooms.existsById(roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        intervalValidator.validate(startsAt, endsAt);
        return new Availability(roomId, startsAt.withOffsetSameInstant(ZoneOffset.UTC),
                endsAt.withOffsetSameInstant(ZoneOffset.UTC), !bookings.hasOverlap(roomId, startsAt, endsAt));
    }
}
