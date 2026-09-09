package com.sber.meetingrooms.integration;

import com.sber.meetingrooms.support.ApiIntegrationTestSupport;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingReplayTimeIntegrationTest extends ApiIntegrationTestSupport {
    @MockitoBean(name = "fixedClock") Clock clock;

    @Test
    void replaysAfterStartButStillValidatesNewAndChangedRequests() throws Exception {
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.instant()).thenReturn(Instant.parse("2030-01-01T08:00:00Z"));
        String body = payload("room-1", "2030-01-01T10:00:00Z", "2030-01-01T11:00:00Z");
        var first = mvc.perform(post("/api/bookings").header("Idempotency-Key", "time-replay")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse();

        when(clock.instant()).thenReturn(Instant.parse("2030-01-01T10:01:00Z"));
        mvc.perform(post("/api/bookings").header("Idempotency-Key", "time-replay")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(content().json(first.getContentAsString()))
                .andExpect(header().string("Location", first.getHeader("Location")));
        mvc.perform(post("/api/bookings").header("Idempotency-Key", "time-replay")
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("room-1", "room-2")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/bookings").header("Idempotency-Key", "time-replay")
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("11:00:00", "10:05:00")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/bookings").header("Idempotency-Key", "new-key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/bookings")).andExpect(jsonPath("$.content.length()").value(1));
    }
}
