package com.example.meetingrooms.validation;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BookingIntervalValidator {
    private final Clock clock;

    public BookingIntervalValidator(Clock clock) {
        this.clock = clock;
    }

    public void validate(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (startsAt == null || endsAt == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both startsAt and endsAt are required");
        }
        if (startsAt.toInstant().isBefore(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking cannot start in the past");
        }
        Duration duration = Duration.between(startsAt.toInstant(), endsAt.toInstant());
        if (duration.compareTo(Duration.ofMinutes(15)) < 0 || duration.compareTo(Duration.ofHours(4)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be between 15 minutes and 4 hours inclusive");
        }
    }
}
