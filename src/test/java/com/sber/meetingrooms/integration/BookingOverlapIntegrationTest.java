package com.sber.meetingrooms.integration;

import com.sber.meetingrooms.support.ApiIntegrationTestSupport;
import com.sber.meetingrooms.generated.model.BookingRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingOverlapIntegrationTest extends ApiIntegrationTestSupport {
    @ParameterizedTest
    @CsvSource({
            "09:30:00,10:30:00", "10:30:00,11:30:00", "10:15:00,10:45:00",
            "09:00:00,12:00:00", "10:00:00,11:00:00"
    })
    void rejectsEveryOverlapShape(String start, String end) throws Exception {
        createDefault();
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("room-1", "2030-01-01T" + start + "Z", "2030-01-01T" + end + "Z")))
                .andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void allowsAdjacentIntervalsAndOtherRooms() throws Exception {
        createDefault();
        for (BookingRequest value : new BookingRequest[]{
                request("room-1", "2030-01-01T09:00:00Z", "2030-01-01T10:00:00Z"),
                request("room-1", "2030-01-01T11:00:00Z", "2030-01-01T12:00:00Z"),
                request("room-2", "2030-01-01T10:00:00Z", "2030-01-01T11:00:00Z")}) {
            mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(value))).andExpect(status().isCreated());
        }
        mvc.perform(get("/api/bookings"))
                .andExpect(jsonPath("$[0].startsAt").value("2030-01-01T09:00:00Z"));
    }
}
