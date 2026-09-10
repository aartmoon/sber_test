package com.sber.meetingrooms.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.booking")
public record BookingPolicyProperties(Duration minDuration, Duration maxDuration) {
    public BookingPolicyProperties {
        if (minDuration == null || minDuration.isZero() || minDuration.isNegative()) {
            throw new IllegalArgumentException("app.booking.min-duration must be positive");
        }
        if (maxDuration == null || maxDuration.compareTo(minDuration) < 0) {
            throw new IllegalArgumentException(
                    "app.booking.max-duration must not be less than min-duration");
        }
    }
}
