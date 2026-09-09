package com.sber.meetingrooms.integration;

import com.sber.meetingrooms.support.ApiIntegrationTestSupport;
import com.sber.meetingrooms.service.BookingService;
import com.sber.meetingrooms.repository.IdempotencyRepository;
import com.sber.meetingrooms.service.IdempotencyCleanupScheduler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingApiEnhancementsIntegrationTest extends ApiIntegrationTestSupport {
    @Autowired BookingService service;
    @Autowired IdempotencyRepository idempotency;
    @Autowired IdempotencyCleanupScheduler idempotencyCleanup;

    @Test
    void servesTheSourceOpenApiContract() throws Exception {
        mvc.perform(get("/openapi.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi: 3.0.3")));
    }

    @Test
    void paginatesBookings() throws Exception {
        for (int hour : IntStream.range(9, 12).toArray()) {
            mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                    .content(payload("room-1", timestamp(hour), timestamp(hour + 1))))
                    .andExpect(status().isCreated());
        }

        var firstPage = mvc.perform(get("/api/v2/bookings").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].startsAt").value(timestamp(9)))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn();
        String cursor = mapper.readTree(firstPage.getResponse().getContentAsString())
                .get("nextCursor").asText();

        mvc.perform(get("/api/v2/bookings").param("cursor", cursor).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].startsAt").value(timestamp(11)))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.nextCursor").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void cursorDoesNotSkipBookingsWithTheSameStartTime() throws Exception {
        for (String room : new String[]{"room-1", "room-2", "room-3"}) {
            mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                            .content(payload(room, timestamp(10), timestamp(11))))
                    .andExpect(status().isCreated());
        }

        var firstPage = mvc.perform(get("/api/v2/bookings").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andReturn();
        var firstJson = mapper.readTree(firstPage.getResponse().getContentAsString());

        var secondPage = mvc.perform(get("/api/v2/bookings")
                        .param("cursor", firstJson.get("nextCursor").asText())
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andReturn();

        var ids = new HashSet<String>();
        firstJson.get("content").forEach(item -> ids.add(item.get("id").asText()));
        mapper.readTree(secondPage.getResponse().getContentAsString()).get("content")
                .forEach(item -> ids.add(item.get("id").asText()));
        assertThat(ids).hasSize(3);
    }

    @Test
    void validatesPaginationBounds() throws Exception {
        mvc.perform(get("/api/v2/bookings").param("cursor", "not-a-valid-cursor"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v2/bookings").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replaysSuccessfulPostForSameIdempotencyKey() throws Exception {
        String body = payload("room-1", timestamp(10), timestamp(11));
        var first = mvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", " booking-123 ")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        String firstId = mapper.readTree(first.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", "booking-123")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(firstId))
                .andExpect(header().string("Location", "/api/bookings/" + firstId));
        mvc.perform(get("/api/bookings"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rejectsReusingIdempotencyKeyForDifferentRequest() throws Exception {
        mvc.perform(post("/api/bookings").header("Idempotency-Key", "same-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("room-1", timestamp(10), timestamp(11))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/bookings").header("Idempotency-Key", "same-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("room-2", timestamp(10), timestamp(11))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void doesNotRecreateCancelledBookingWhenOriginalRequestIsRetried() throws Exception {
        String body = payload("room-1", timestamp(10), timestamp(11));
        var first = mvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", "cancelled-booking")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String firstId = mapper.readTree(first.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(delete("/api/bookings/{id}", firstId))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", "cancelled-booking")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "The booking created with this Idempotency-Key was cancelled"));
        mvc.perform(get("/api/bookings"))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void allowsKeyReuseAfterRetentionPeriod() throws Exception {
        String body = payload("room-1", timestamp(10), timestamp(11));
        var first = mvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", "expired-key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String firstId = mapper.readTree(first.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(delete("/api/bookings/{id}", firstId)).andExpect(status().isNoContent());
        var stored = idempotency.findById("expired-key").orElseThrow();
        stored.expireAt(java.time.OffsetDateTime.parse("2030-01-01T07:59:59Z"));
        idempotency.saveAndFlush(stored);
        idempotencyCleanup.cleanupExpired();

        var replay = mvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", "expired-key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String replayId = mapper.readTree(replay.getResponse().getContentAsString()).get("id").asText();

        assertThat(replayId).isNotEqualTo(firstId);
        mvc.perform(get("/api/bookings"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void concurrentRetriesReturnOneBooking() throws Exception {
        int workers = 8;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(workers)) {
            var futures = new ArrayList<java.util.concurrent.Future<String>>();
            for (int i = 0; i < workers; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Start timeout");
                    }
                    return service.create(command("room-1", timestamp(10), timestamp(11)),
                            "concurrent-key").id().toString();
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var ids = new ArrayList<String>();
            for (var future : futures) {
                ids.add(future.get(15, TimeUnit.SECONDS));
            }
            assertThat(ids).containsOnly(ids.getFirst());
            assertThat(service.list(null, 20).content()).hasSize(1);
        }
    }

    @Test
    void normalizesDomainFields() throws Exception {
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("  room-1  ", timestamp(10), timestamp(11))
                                .replace("employee@example.com", "Employee@Example.COM")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value("room-1"))
                .andExpect(jsonPath("$.employeeEmail").value("employee@example.com"));
    }

    private String timestamp(int hour) {
        return "2030-01-01T" + String.format("%02d", hour) + ":00:00Z";
    }
}
