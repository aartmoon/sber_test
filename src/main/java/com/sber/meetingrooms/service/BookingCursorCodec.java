package com.sber.meetingrooms.service;

import com.sber.meetingrooms.exception.InvalidRequestException;
import com.sber.meetingrooms.model.Booking;
import java.nio.ByteBuffer;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BookingCursorCodec {
    private static final byte VERSION = 1;
    private static final int CURSOR_BYTES = 29;

    public String encode(Booking booking) {
        Instant instant = booking.startsAt().toInstant();
        ByteBuffer payload = ByteBuffer.allocate(CURSOR_BYTES)
                .put(VERSION)
                .putLong(instant.getEpochSecond())
                .putInt(instant.getNano())
                .putLong(booking.id().getMostSignificantBits())
                .putLong(booking.id().getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.array());
    }

    public Position decode(String cursor) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            if (decoded.length != CURSOR_BYTES) {
                throw new IllegalArgumentException("Unexpected cursor length");
            }
            ByteBuffer payload = ByteBuffer.wrap(decoded);
            if (payload.get() != VERSION) {
                throw new IllegalArgumentException("Unsupported cursor version");
            }
            Instant instant = Instant.ofEpochSecond(payload.getLong(), payload.getInt());
            UUID id = new UUID(payload.getLong(), payload.getLong());
            return new Position(OffsetDateTime.ofInstant(instant, ZoneOffset.UTC), id);
        } catch (IllegalArgumentException | DateTimeException ex) {
            throw invalidCursor();
        }
    }

    private InvalidRequestException invalidCursor() {
        return new InvalidRequestException("Invalid booking cursor");
    }

    public record Position(OffsetDateTime startsAt, UUID id) {
    }
}
