package com.example.meetingrooms.integration;

import com.example.meetingrooms.support.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingValidationIntegrationTest extends ApiIntegrationTestSupport {
    @ParameterizedTest
    @CsvSource({"07:59:59,09:00:00", "10:00:00,10:14:59", "10:00:00,14:00:01", "10:00:00,10:00:00", "11:00:00,10:00:00"})
    void rejectsInvalidIntervalsInBothEndpoints(String start, String end) throws Exception {
        String startsAt = "2030-01-01T" + start + "Z";
        String endsAt = "2030-01-01T" + end + "Z";
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                .content(payload("room-1", startsAt, endsAt))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/rooms/room-1/availability").param("startsAt", startsAt).param("endsAt", endsAt))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({"08:00:00,08:15:00", "10:00:00,14:00:00"})
    void acceptsInclusiveDurationAndCurrentTimeBoundaries(String start, String end) throws Exception {
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                .content(payload("room-1", "2030-01-01T" + start + "Z", "2030-01-01T" + end + "Z")))
                .andExpect(status().isCreated());
    }

    @Test
    void validatesInputAndUnknownResources() throws Exception {
        for (String body : new String[]{"{}", "{", "null",
                "{\"roomId\":\"room-1\",\"employeeEmail\":\"bad\",\"startsAt\":\"2030-01-01T10:00:00Z\",\"endsAt\":\"2030-01-01T11:00:00Z\"}",
                "{\"roomId\":\"room-1\",\"employeeEmail\":\"a@b.com\",\"startsAt\":\"2030-01-01T10:00:00\",\"endsAt\":\"2030-01-01T11:00:00Z\"}"}) {
            mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                .content(payload("missing", "2030-01-01T10:00:00Z", "2030-01-01T11:00:00Z")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/rooms/missing/availability").param("startsAt", "2030-01-01T10:00:00Z")
                .param("endsAt", "2030-01-01T11:00:00Z")).andExpect(status().isNotFound());
        mvc.perform(get("/api/rooms/room-1/availability")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/rooms/room-1/availability").param("startsAt", "invalid").param("endsAt", "invalid"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/bookings/not-a-uuid")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/bookings/00000000-0000-0000-0000-000000000000")).andExpect(status().isNotFound());
    }
}
