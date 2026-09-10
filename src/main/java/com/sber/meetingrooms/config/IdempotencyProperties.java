package com.sber.meetingrooms.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.idempotency")
public record IdempotencyProperties(Duration retention) {
    public IdempotencyProperties {
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("app.idempotency.retention must be positive");
        }
    }
}
