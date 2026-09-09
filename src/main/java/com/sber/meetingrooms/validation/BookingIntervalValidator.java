package com.sber.meetingrooms.validation;

import com.sber.meetingrooms.exception.InvalidRequestException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class BookingIntervalValidator {
    private final Clock clock;

    public BookingIntervalValidator(Clock clock) {
        this.clock = clock;
    }

    public void validate(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (startsAt == null || endsAt == null) {
            throw new InvalidRequestException("Both startsAt and endsAt are required");
        }
        Duration duration = Duration.between(startsAt.toInstant(), endsAt.toInstant());
        if (duration.compareTo(Duration.ofMinutes(15)) < 0 || duration.compareTo(Duration.ofHours(4)) > 0) {
            throw new InvalidRequestException("Duration must be between 15 minutes and 4 hours inclusive");
        }
        validateNotPast(startsAt);
    }

    public void validateNotPast(OffsetDateTime startsAt) {
        if (startsAt.toInstant().isBefore(clock.instant())) {
            throw new InvalidRequestException("Booking cannot start in the past");
        }
    }
}
