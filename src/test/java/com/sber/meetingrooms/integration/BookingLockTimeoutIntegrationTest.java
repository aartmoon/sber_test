package com.sber.meetingrooms.integration;

import com.sber.meetingrooms.repository.RoomRepository;
import com.sber.meetingrooms.repository.IdempotencyRepository;
import com.sber.meetingrooms.model.Booking;
import com.sber.meetingrooms.model.BookingIdempotency;
import com.sber.meetingrooms.support.ApiIntegrationTestSupport;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:meetingrooms-lock-test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=100")
class BookingLockTimeoutIntegrationTest extends ApiIntegrationTestSupport {
    @Autowired RoomRepository rooms;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired IdempotencyRepository idempotency;

    @Test
    void returnsRetryableServiceUnavailableWhenIdempotencyInsertTimesOut() throws Exception {
        var reserved = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var start = OffsetDateTime.parse("2030-01-01T10:00:00Z");
        var booking = new Booking(UUID.randomUUID(), "room-1", "employee@example.com",
                start, start.plusHours(1), start.minusHours(2));

        try (var executor = Executors.newSingleThreadExecutor()) {
            var holder = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        status.setRollbackOnly();
                        idempotency.insertAndFlush(new BookingIdempotency(
                                "busy-key", "pending", booking, start.plusHours(24)));
                        reserved.countDown();
                        try {
                            if (!release.await(5, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("Release timeout");
                            }
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(ex);
                        }
                    }));
            try {
                assertThat(reserved.await(5, TimeUnit.SECONDS)).isTrue();
                mvc.perform(post("/api/bookings")
                                .header("Idempotency-Key", "busy-key")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload("room-1", start.toString(), start.plusHours(1).toString())))
                        .andExpect(status().isServiceUnavailable())
                        .andExpect(header().string("Retry-After", "1"));
            } finally {
                release.countDown();
            }
            holder.get(5, TimeUnit.SECONDS);
        }

        assertThat(idempotency.findById("busy-key")).isEmpty();
        mvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", "busy-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("room-1", start.toString(), start.plusHours(1).toString())))
                .andExpect(status().isCreated());
    }

    @Test
    void returnsRetryableServiceUnavailableWhenRoomLockTimesOut() throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (var executor = Executors.newSingleThreadExecutor()) {
            var holder = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        rooms.findByIdForUpdate("room-1").orElseThrow();
                        locked.countDown();
                        try {
                            if (!release.await(5, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("Lock release timeout");
                            }
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Interrupted while holding room lock", ex);
                        }
                    }));

            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            try {
                mvc.perform(post("/api/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload("room-1", "2030-01-01T10:00:00Z",
                                        "2030-01-01T11:00:00Z")))
                        .andExpect(status().isServiceUnavailable())
                        .andExpect(header().string("Retry-After", "1"))
                        .andExpect(jsonPath("$.detail").value(
                                "Room is busy processing another request; retry shortly"));
            } finally {
                release.countDown();
            }
            holder.get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void rejectsPastStartWithoutWaitingForRoomLock() throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (var executor = Executors.newSingleThreadExecutor()) {
            var holder = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        rooms.findByIdForUpdate("room-1").orElseThrow();
                        locked.countDown();
                        try {
                            if (!release.await(5, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("Lock release timeout");
                            }
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Interrupted while holding room lock", ex);
                        }
                    }));

            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            try {
                mvc.perform(post("/api/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload("room-1", "2030-01-01T07:00:00Z",
                                        "2030-01-01T08:00:00Z")))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.detail").value("Booking cannot start in the past"));
            } finally {
                release.countDown();
            }
            holder.get(5, TimeUnit.SECONDS);
        }
    }
}
