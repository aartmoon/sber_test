package com.example.meetingrooms.unit.service;

import com.example.meetingrooms.repository.BookingRepository;
import com.example.meetingrooms.repository.RoomRepository;
import com.example.meetingrooms.service.AvailabilityService;
import com.example.meetingrooms.validation.BookingIntervalValidator;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {
    @Mock BookingRepository bookings;
    @Mock RoomRepository rooms;
    @Mock BookingIntervalValidator validator;
    private AvailabilityService service;
    private final OffsetDateTime start = OffsetDateTime.parse("2030-01-01T13:00:00+03:00");
    private final OffsetDateTime end = OffsetDateTime.parse("2030-01-01T14:00:00+03:00");

    @BeforeEach
    void setUp() {
        service = new AvailabilityService(bookings, rooms, validator);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void reportsAvailabilityAndUtcTimesWithoutChangingBookings(boolean overlap) {
        when(rooms.existsById("room-1")).thenReturn(true);
        when(bookings.hasOverlap("room-1", start, end)).thenReturn(overlap);

        var result = service.availability("room-1", start, end);

        assertThat(result.available()).isEqualTo(!overlap);
        assertThat(result.roomId()).isEqualTo("room-1");
        assertThat(result.startsAt()).isEqualTo(OffsetDateTime.parse("2030-01-01T10:00:00Z"));
        assertThat(result.startsAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(result.endsAt()).isEqualTo(OffsetDateTime.parse("2030-01-01T11:00:00Z"));
        assertThat(result.endsAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        verify(validator).validate(start, end);
        verify(rooms).existsById("room-1");
        verify(bookings).hasOverlap("room-1", start, end);
        verifyNoMoreInteractions(rooms, bookings);
    }

    @Test
    void rejectsUnknownRoomBeforeCheckingBookings() {
        assertThatThrownBy(() -> service.availability("missing", start, end))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(validator, bookings);
    }

    @Test
    void rejectsInvalidIntervalBeforeCheckingBookings() {
        when(rooms.existsById("room-1")).thenReturn(true);
        var invalid = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid interval");
        doThrow(invalid).when(validator).validate(start, end);

        assertThatThrownBy(() -> service.availability("room-1", start, end)).isSameAs(invalid);
        verifyNoInteractions(bookings);
    }
}
