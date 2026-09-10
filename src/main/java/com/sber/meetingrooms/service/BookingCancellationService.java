package com.sber.meetingrooms.service;

import com.sber.meetingrooms.exception.ResourceNotFoundException;
import com.sber.meetingrooms.repository.BookingRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingCancellationService {
    private final BookingRepository bookings;
    private final Clock clock;

    public BookingCancellationService(BookingRepository bookings, Clock clock) {
        this.bookings = bookings;
        this.clock = clock;
    }

    @Transactional
    public void cancel(UUID id) {
        if (bookings.cancelById(id, OffsetDateTime.now(clock)) == 0) {
            throw new ResourceNotFoundException("Booking not found");
        }
    }
}
