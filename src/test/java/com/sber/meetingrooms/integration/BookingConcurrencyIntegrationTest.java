package com.sber.meetingrooms.integration;

import com.sber.meetingrooms.support.ApiIntegrationTestSupport;
import com.sber.meetingrooms.service.BookingService;
import com.sber.meetingrooms.exception.ConflictException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.assertThat;

class BookingConcurrencyIntegrationTest extends ApiIntegrationTestSupport {
    @Autowired BookingService service;

    @Test
    void concurrentRequestsCreateExactlyOneBooking() throws Exception {
        int workers = 8;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(workers)) {
            var futures = new ArrayList<java.util.concurrent.Future<HttpStatus>>();
            Callable<HttpStatus> attempt = () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
                try {
                    service.create(command("room-1", "2030-01-01T10:00:00Z", "2030-01-01T11:00:00Z"), null);
                    return HttpStatus.CREATED;
                } catch (ConflictException ex) {
                    return HttpStatus.CONFLICT;
                }
            };
            for (int i = 0; i < workers; i++) futures.add(executor.submit(attempt));
            boolean allReady = ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            assertThat(allReady).isTrue();
            var results = new ArrayList<HttpStatus>();
            for (var future : futures) results.add(future.get(15, TimeUnit.SECONDS));
            assertThat(results).filteredOn(HttpStatus.CREATED::equals).hasSize(1);
            assertThat(results).filteredOn(HttpStatus.CONFLICT::equals).hasSize(workers - 1);
            assertThat(service.list(null, 20).content()).hasSize(1);
        }
    }
}
