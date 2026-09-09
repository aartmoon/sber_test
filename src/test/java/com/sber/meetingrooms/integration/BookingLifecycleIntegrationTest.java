package com.sber.meetingrooms.integration;

import com.sber.meetingrooms.support.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingLifecycleIntegrationTest extends ApiIntegrationTestSupport {
    @Test
    void fullLifecycle() throws Exception {
        mvc.perform(get("/api/bookings")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.hasMore").value(false));
        String id = createDefault().get("id").asText();
        mvc.perform(get("/api/bookings")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
        availability("room-1", "2030-01-01T10:15:00Z", "2030-01-01T10:45:00Z", false);
        mvc.perform(delete("/api/bookings/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        availability("room-1", "2030-01-01T10:15:00Z", "2030-01-01T10:45:00Z", true);
        mvc.perform(delete("/api/bookings/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/bookings")).andExpect(jsonPath("$.content").isEmpty());
    }
}
