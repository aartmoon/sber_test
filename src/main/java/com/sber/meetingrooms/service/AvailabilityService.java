package com.sber.meetingrooms.service;

import com.sber.meetingrooms.exception.ResourceNotFoundException;
import com.sber.meetingrooms.model.RoomAvailability;
import com.sber.meetingrooms.repository.BookingRepository;
import com.sber.meetingrooms.repository.RoomRepository;
import com.sber.meetingrooms.validation.BookingIntervalValidator;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityService {
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final BookingIntervalValidator intervalValidator;
    private final RequestNormalizer normalizer;

    public AvailabilityService(BookingRepository bookings, RoomRepository rooms,
                               BookingIntervalValidator intervalValidator,
                               RequestNormalizer normalizer) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.intervalValidator = intervalValidator;
        this.normalizer = normalizer;
    }

    public RoomAvailability availability(String roomId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        roomId = normalizer.normalizeRoomId(roomId);
        if (!rooms.existsById(roomId)) {
            throw new ResourceNotFoundException("Room not found");
        }
        intervalValidator.validate(startsAt, endsAt);
        return new RoomAvailability(roomId, startsAt.withOffsetSameInstant(ZoneOffset.UTC),
                endsAt.withOffsetSameInstant(ZoneOffset.UTC), !bookings.hasOverlap(roomId, startsAt, endsAt));
    }
}
