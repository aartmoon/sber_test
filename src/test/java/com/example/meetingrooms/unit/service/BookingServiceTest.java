package com.example.meetingrooms.unit.service;

import com.example.meetingrooms.dto.BookingRequest;
import com.example.meetingrooms.model.Booking;
import com.example.meetingrooms.repository.BookingRepository;
import com.example.meetingrooms.repository.RoomRepository;
import com.example.meetingrooms.service.BookingService;
import com.example.meetingrooms.validation.BookingIntervalValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock BookingRepository bookings;
    @Mock RoomRepository rooms;
    @Mock BookingIntervalValidator validator;
    private BookingService service;
    private final Clock clock = Clock.fixed(Instant.parse("2030-01-01T08:00:00Z"), ZoneOffset.UTC);
    private final BookingRequest request = new BookingRequest("room-1", "employee@example.com",
            OffsetDateTime.parse("2030-01-01T13:00:00+03:00"),
            OffsetDateTime.parse("2030-01-01T14:00:00+03:00"));

    @BeforeEach
    void setUp() {
        service = new BookingService(bookings, rooms, validator, clock);
    }

    @Test
    void createsBookingWithUtcTimesAndLocksRoomBeforeCheckingOverlap() {
        when(rooms.lockById(request.roomId())).thenReturn(true);

        Booking result = service.create(request);

        assertThat(result.id()).isNotNull();
        assertThat(result.roomId()).isEqualTo(request.roomId());
        assertThat(result.employeeEmail()).isEqualTo(request.employeeEmail());
        assertThat(result.startsAt()).isEqualTo(OffsetDateTime.parse("2030-01-01T10:00:00Z"));
        assertThat(result.startsAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(result.endsAt()).isEqualTo(OffsetDateTime.parse("2030-01-01T11:00:00Z"));
        assertThat(result.endsAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(result.createdAt()).isEqualTo(OffsetDateTime.now(clock));
        var order = inOrder(rooms, validator, bookings);
        order.verify(rooms).lockById(request.roomId());
        order.verify(validator).validate(request.startsAt(), request.endsAt());
        order.verify(bookings).hasOverlap(request.roomId(), request.startsAt(), request.endsAt());
        order.verify(bookings).save(result);
    }

    @Test
    void doesNotPersistBookingForUnknownRoom() {
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(validator, bookings);
    }

    @Test
    void doesNotCheckOverlapOrPersistInvalidInterval() {
        when(rooms.lockById(request.roomId())).thenReturn(true);
        var invalid = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid interval");
        doThrow(invalid).when(validator).validate(request.startsAt(), request.endsAt());

        assertThatThrownBy(() -> service.create(request)).isSameAs(invalid);
        verifyNoInteractions(bookings);
    }

    @Test
    void doesNotPersistOverlappingBooking() {
        when(rooms.lockById(request.roomId())).thenReturn(true);
        when(bookings.hasOverlap(request.roomId(), request.startsAt(), request.endsAt())).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(bookings, never()).save(any());
    }

    @Test
    void returnsBookingsInRepositoryOrder() {
        Booking first = new Booking(UUID.randomUUID(), request.roomId(), request.employeeEmail(),
                request.startsAt(), request.endsAt(), OffsetDateTime.now(clock));
        Booking second = new Booking(UUID.randomUUID(), "room-2", request.employeeEmail(),
                request.startsAt().plusHours(2), request.endsAt().plusHours(2), OffsetDateTime.now(clock));
        when(bookings.findAll()).thenReturn(List.of(first, second));

        assertThat(service.list()).containsExactly(first, second);
    }

    @Test
    void cancelsExistingBooking() {
        UUID id = UUID.randomUUID();
        when(bookings.deleteById(id)).thenReturn(true);

        assertThatCode(() -> service.cancel(id)).doesNotThrowAnyException();
        verify(bookings).deleteById(id);
    }

    @Test
    void reportsMissingBookingOnCancellation() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.cancel(id))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
