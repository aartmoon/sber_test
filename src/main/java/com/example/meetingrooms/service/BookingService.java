package com.example.meetingrooms.service;

import com.example.meetingrooms.dto.BookingRequest;
import com.example.meetingrooms.model.Booking;
import com.example.meetingrooms.repository.BookingRepository;
import com.example.meetingrooms.repository.RoomRepository;
import com.example.meetingrooms.validation.BookingIntervalValidator;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingService {
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final BookingIntervalValidator intervalValidator;
    private final Clock clock;

    public BookingService(BookingRepository bookings, RoomRepository rooms,
                          BookingIntervalValidator intervalValidator, Clock clock) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.intervalValidator = intervalValidator;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Booking create(BookingRequest request) {
        if (!rooms.lockById(request.roomId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        intervalValidator.validate(request.startsAt(), request.endsAt());
        if (bookings.hasOverlap(request.roomId(), request.startsAt(), request.endsAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is already booked for this interval");
        }
        Booking booking = new Booking(UUID.randomUUID(), request.roomId(), request.employeeEmail(),
                request.startsAt().withOffsetSameInstant(ZoneOffset.UTC),
                request.endsAt().withOffsetSameInstant(ZoneOffset.UTC), OffsetDateTime.now(clock));
        bookings.save(booking);
        return booking;
    }

    public List<Booking> list() {
        return bookings.findAll();
    }

    public void cancel(UUID id) {
        if (!bookings.deleteById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
    }
}
