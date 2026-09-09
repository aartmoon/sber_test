package com.example.meetingrooms.integration;

import com.example.meetingrooms.support.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingLifecycleIntegrationTest extends ApiIntegrationTestSupport {
    @Test
    void fullLifecycle() throws Exception {
        mvc.perform(get("/api/bookings")).andExpect(status().isOk()).andExpect(content().json("[]"));
        String id = createDefault().get("id").asText();
        mvc.perform(get("/api/bookings")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(id));
        availability("room-1", "2030-01-01T10:15:00Z", "2030-01-01T10:45:00Z", false);
        mvc.perform(delete("/api/bookings/{id}", id)).andExpect(status().isNoContent()).andExpect(content().string(""));
        availability("room-1", "2030-01-01T10:15:00Z", "2030-01-01T10:45:00Z", true);
        mvc.perform(delete("/api/bookings/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/bookings")).andExpect(content().json("[]"));
    }
}
