package com.example.meetingrooms.controller;

import com.example.meetingrooms.model.Booking;
import com.example.meetingrooms.dto.BookingRequest;
import com.example.meetingrooms.service.BookingService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BookingController {
    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping("/bookings")
    public ResponseEntity<Booking> create(@Valid @RequestBody BookingRequest request) {
        Booking booking = service.create(request);
        return ResponseEntity.created(URI.create("/api/bookings/" + booking.id())).body(booking);
    }

    @GetMapping("/bookings")
    public List<Booking> list() {
        return service.list();
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        service.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
