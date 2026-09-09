package com.example.meetingrooms.integration;

import com.example.meetingrooms.support.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AvailabilityIntegrationTest extends ApiIntegrationTestSupport {
    @Test
    void comparesInstantsAcrossTimeZones() throws Exception {
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("room-1", "2030-01-01T13:00:00+03:00", "2030-01-01T14:00:00+03:00")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.startsAt").value("2030-01-01T10:00:00Z"));
        availability("room-1", "2030-01-01T10:00:00Z", "2030-01-01T11:00:00Z", false);
    }
}
