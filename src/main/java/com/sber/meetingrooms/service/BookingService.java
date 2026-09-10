package com.sber.meetingrooms.service;

import com.sber.meetingrooms.model.Booking;
import com.sber.meetingrooms.model.BookingPageResult;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Facade for booking use cases. */
@Service
public class BookingService {
    private final BookingCreationService creation;
    private final BookingQueryService queries;
    private final BookingCancellationService cancellation;

    public BookingService(BookingCreationService creation, BookingQueryService queries,
                          BookingCancellationService cancellation) {
        this.creation = creation;
        this.queries = queries;
        this.cancellation = cancellation;
    }

    public Booking create(CreateBookingCommand command, String idempotencyKey) {
        return creation.create(command, idempotencyKey);
    }

    public List<Booking> listAll() {
        return queries.listAll();
    }

    public BookingPageResult list(String cursor, int size) {
        return queries.list(cursor, size);
    }

    public void cancel(UUID id) {
        cancellation.cancel(id);
    }
}
