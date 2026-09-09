package com.sber.meetingrooms.support;

import com.sber.meetingrooms.generated.model.BookingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.sber.meetingrooms.repository.BookingRepository;
import com.sber.meetingrooms.repository.IdempotencyRepository;
import com.sber.meetingrooms.service.CreateBookingCommand;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfig.class)
public abstract class ApiIntegrationTestSupport {
    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper mapper;
    @Autowired private BookingRepository bookings;
    @Autowired private IdempotencyRepository idempotency;

    @BeforeEach
    void clearBookings() {
        idempotency.deleteAllInBatch();
        bookings.deleteAllInBatch();
    }

    protected BookingRequest request(String room, String start, String end) {
        return new BookingRequest(room, "employee@example.com", OffsetDateTime.parse(start), OffsetDateTime.parse(end));
    }

    protected CreateBookingCommand command(String room, String start, String end) {
        return new CreateBookingCommand(room, "employee@example.com",
                OffsetDateTime.parse(start), OffsetDateTime.parse(end));
    }

    protected String payload(String room, String start, String end) throws Exception {
        return mapper.writeValueAsString(request(room, start, end));
    }

    protected JsonNode createDefault() throws Exception {
        var result = mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("room-1", "2030-01-01T10:00:00Z", "2030-01-01T11:00:00Z")))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.roomId").value("room-1"))
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    protected void availability(String room, String start, String end, boolean available) throws Exception {
        mvc.perform(get("/api/rooms/{room}/availability", room).param("startsAt", start).param("endsAt", end))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(available));
    }
}
