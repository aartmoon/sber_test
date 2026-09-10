package com.sber.meetingrooms.validation;

import com.sber.meetingrooms.config.BookingPolicyProperties;
import com.sber.meetingrooms.exception.InvalidRequestException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class BookingIntervalValidator {
    private final Clock clock;
    private final BookingPolicyProperties policy;

    public BookingIntervalValidator(Clock clock, BookingPolicyProperties policy) {
        this.clock = clock;
        this.policy = policy;
    }

    public void validate(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        validateStructure(startsAt, endsAt);
        validateNotPast(startsAt);
    }

    public void validateStructure(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (startsAt == null || endsAt == null) {
            throw new InvalidRequestException("Both startsAt and endsAt are required");
        }
        Duration duration = Duration.between(startsAt.toInstant(), endsAt.toInstant());
        if (duration.compareTo(policy.minDuration()) < 0
                || duration.compareTo(policy.maxDuration()) > 0) {
            throw new InvalidRequestException("Duration must be between " + policy.minDuration()
                    + " and " + policy.maxDuration() + " inclusive");
        }
    }

    public void validateNotPast(OffsetDateTime startsAt) {
        if (startsAt.toInstant().isBefore(clock.instant())) {
            throw new InvalidRequestException("Booking cannot start in the past");
        }
    }
}
