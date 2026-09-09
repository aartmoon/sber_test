package com.sber.meetingrooms.service;

import com.sber.meetingrooms.exception.InvalidRequestException;
import java.time.OffsetDateTime;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class RequestNormalizer {
    public NormalizedBooking normalize(CreateBookingCommand command) {
        return new NormalizedBooking(normalizeRoomId(command.roomId()),
                command.employeeEmail().toLowerCase(Locale.ROOT),
                command.startsAt(), command.endsAt());
    }

    public String normalizeRoomId(String roomId) {
        if (roomId == null) {
            return null;
        }
        String normalized = roomId.strip();
        if (normalized.isEmpty()) {
            throw new InvalidRequestException("roomId must not be blank");
        }
        return normalized;
    }

    public String normalizeIdempotencyKey(String key) {
        if (key == null) {
            return null;
        }
        String normalized = key.strip();
        if (normalized.isEmpty()) {
            throw new InvalidRequestException("Idempotency-Key must not be blank");
        }
        return normalized;
    }

    public record NormalizedBooking(String roomId, String employeeEmail,
                                    OffsetDateTime startsAt, OffsetDateTime endsAt) {
    }
}
