package com.sber.meetingrooms.controller;

import com.sber.meetingrooms.generated.api.BookingsApi;
import com.sber.meetingrooms.generated.model.BookingPage;
import com.sber.meetingrooms.generated.model.BookingRequest;
import com.sber.meetingrooms.generated.model.BookingResponse;
import com.sber.meetingrooms.service.BookingService;
import com.sber.meetingrooms.service.CreateBookingCommand;
import java.net.URI;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController implements BookingsApi {
    private final BookingService service;
    private final ApiModelMapper mapper;

    public BookingController(BookingService service, ApiModelMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<BookingResponse> createBooking(
            BookingRequest request, @Nullable String idempotencyKey) {
        var booking = service.create(new CreateBookingCommand(
                request.getRoomId(), request.getEmployeeEmail(),
                request.getStartsAt(), request.getEndsAt()), idempotencyKey);
        return ResponseEntity.created(URI.create("/api/bookings/" + booking.id()))
                .body(mapper.toResponse(booking));
    }

    @Override
    public ResponseEntity<BookingPage> listBookings(String cursor, Integer size) {
        return ResponseEntity.ok(mapper.toResponse(service.list(cursor, size)));
    }

    // A bodyless success must not inherit the generated error-only produces constraint.
    @Override
    @DeleteMapping("/api/bookings/{id}")
    public ResponseEntity<Void> cancelBooking(UUID id) {
        service.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
