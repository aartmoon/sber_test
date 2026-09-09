package com.sber.meetingrooms.integration;

import com.sber.meetingrooms.exception.ConflictException;
import com.sber.meetingrooms.exception.ResourceNotFoundException;
import com.sber.meetingrooms.model.Booking;
import com.sber.meetingrooms.repository.IdempotencyRepository;
import com.sber.meetingrooms.repository.IdempotencyInsertRepositoryImpl;
import com.sber.meetingrooms.service.BookingService;
import com.sber.meetingrooms.support.ApiIntegrationTestSupport;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

class IdempotencyConcurrencyIntegrationTest extends ApiIntegrationTestSupport {
    @Autowired BookingService service;
    @Autowired IdempotencyRepository idempotency;
    @MockitoSpyBean IdempotencyInsertRepositoryImpl inserts;

    @Test
    void concurrentIdenticalRequestsReturnTheSameBooking() throws Exception {
        var results = race("room-1", "same-key", "same-key");

        assertThat(results).allMatch(Booking.class::isInstance);
        assertThat(((Booking) results.getFirst()).id()).isEqualTo(((Booking) results.getLast()).id());
        assertThat(service.list(null, 20).content()).hasSize(1);
        assertThat(idempotency.count()).isEqualTo(1);
    }

    @Test
    void concurrentSameKeyInDifferentRoomsReturnsConflictWithoutExtraBooking() throws Exception {
        var results = race("room-2", "same-key", "same-key");

        assertThat(results).filteredOn(Booking.class::isInstance).hasSize(1);
        assertThat(results).filteredOn(ConflictException.class::isInstance).hasSize(1);
        assertThat(service.list(null, 20).content()).hasSize(1);
        assertThat(idempotency.count()).isEqualTo(1);
    }

    @Test
    void differentKeysWithTheSameHashCreateIndependentBookings() throws Exception {
        assertThat("Aa".hashCode()).isEqualTo("BB".hashCode());
        var results = race("room-2", "Aa", "BB");

        assertThat(results).allMatch(Booking.class::isInstance);
        assertThat(service.list(null, 20).content()).hasSize(2);
        assertThat(idempotency.count()).isEqualTo(2);
    }

    @Test
    void failedCreationDoesNotConsumeTheKey() {
        assertThatThrownBy(() -> service.create(
                command("unknown-room", "2030-01-01T10:00:00Z", "2030-01-01T11:00:00Z"), "retry-key"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(idempotency.findById("retry-key")).isEmpty();

        var booking = service.create(
                command("room-1", "2030-01-01T10:00:00Z", "2030-01-01T11:00:00Z"), "retry-key");
        assertThat(idempotency.findById("retry-key").orElseThrow().booking().id()).isEqualTo(booking.id());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void waitingRequestRecoversAfterOwnerCommitsOrRollsBack(boolean rollback) throws Exception {
        var reserved = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var waiting = new CountDownLatch(1);
        var attempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (attempts.getAndIncrement() == 0) {
                invocation.callRealMethod();
                reserved.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Release timeout");
                return null;
            }
            waiting.countDown();
            return invocation.callRealMethod();
        }).when(inserts).insertAndFlush(any());

        try (var executor = Executors.newFixedThreadPool(2)) {
            var owner = executor.submit(() -> {
                try {
                    return (Object) service.create(command(rollback ? "unknown-room" : "room-1",
                            "2030-01-01T10:00:00Z", "2030-01-01T11:00:00Z"), "pending-key");
                } catch (ResourceNotFoundException ex) {
                    return ex;
                }
            });
            try {
                assertThat(reserved.await(5, TimeUnit.SECONDS)).isTrue();
                var follower = executor.submit(attempt("room-1", "pending-key"));
                assertThat(waiting.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> follower.get(100, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);
                release.countDown();

                var result = (Booking) follower.get(5, TimeUnit.SECONDS);
                var original = owner.get(5, TimeUnit.SECONDS);
                if (rollback) {
                    assertThat(original).isInstanceOf(ResourceNotFoundException.class);
                } else {
                    assertThat(result.id()).isEqualTo(((Booking) original).id());
                }
                assertThat(service.list(null, 20).content()).hasSize(1);
                assertThat(idempotency.findById("pending-key").orElseThrow().booking().id())
                        .isEqualTo(result.id());
            } finally {
                release.countDown();
            }
        }
    }

    private List<Object> race(String secondRoom, String firstKey, String secondKey) throws Exception {
        // Both transactions must observe an absent key before attempting their inserts.
        var inserting = new CyclicBarrier(2);
        doAnswer(invocation -> {
            inserting.await(5, TimeUnit.SECONDS);
            return invocation.callRealMethod();
        }).when(inserts).insertAndFlush(any());

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(attempt("room-1", firstKey));
            var second = executor.submit(attempt(secondRoom, secondKey));
            return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }
    }

    private Callable<Object> attempt(String room, String key) {
        return () -> {
            try {
                return service.create(
                        command(room, "2030-01-01T10:00:00Z", "2030-01-01T11:00:00Z"), key);
            } catch (ConflictException ex) {
                return ex;
            }
        };
    }
}
