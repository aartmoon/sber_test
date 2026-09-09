package com.sber.meetingrooms.service;

import com.sber.meetingrooms.repository.IdempotencyRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IdempotencyCleanupScheduler {
    private final IdempotencyRepository idempotency;
    private final Clock clock;

    public IdempotencyCleanupScheduler(IdempotencyRepository idempotency, Clock clock) {
        this.idempotency = idempotency;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.idempotency.cleanup-cron:0 0 * * * *}")
    @Transactional
    public void cleanupExpired() {
        idempotency.deleteExpired(OffsetDateTime.now(clock));
    }
}
