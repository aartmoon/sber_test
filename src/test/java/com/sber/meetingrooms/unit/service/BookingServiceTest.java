package com.sber.meetingrooms.unit.service;

import com.sber.meetingrooms.exception.ConflictException;
import com.sber.meetingrooms.exception.InvalidRequestException;
import com.sber.meetingrooms.exception.ResourceNotFoundException;
import com.sber.meetingrooms.model.Booking;
import com.sber.meetingrooms.repository.BookingRepository;
import com.sber.meetingrooms.repository.IdempotencyRepository;
import com.sber.meetingrooms.repository.RoomRepository;
import com.sber.meetingrooms.service.BookingService;
import com.sber.meetingrooms.service.BookingFingerprint;
import com.sber.meetingrooms.service.BookingCursorCodec;
import com.sber.meetingrooms.service.RequestNormalizer;
import com.sber.meetingrooms.service.CreateBookingCommand;
import com.sber.meetingrooms.validation.BookingIntervalValidator;
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
import org.springframework.data.domain.Pageable;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock BookingRepository bookings;
    @Mock RoomRepository rooms;
    @Mock BookingIntervalValidator validator;
    @Mock IdempotencyRepository idempotency;
    private BookingService service;
    private final Clock clock = Clock.fixed(Instant.parse("2030-01-01T08:00:00Z"), ZoneOffset.UTC);
    private final CreateBookingCommand request = new CreateBookingCommand("room-1", "employee@example.com",
            OffsetDateTime.parse("2030-01-01T13:00:00+03:00"),
            OffsetDateTime.parse("2030-01-01T14:00:00+03:00"));

    @BeforeEach
    void setUp() {
        var normalizer = new RequestNormalizer();
        service = new BookingService(bookings, rooms, validator, idempotency,
                normalizer, new BookingFingerprint(), new BookingCursorCodec(), clock);
    }

    @Test
    void createsBookingWithUtcTimesAndLocksRoomBeforeCheckingOverlap() {
        when(rooms.lockById(request.roomId())).thenReturn(true);

        Booking result = service.create(request, null);

        assertThat(result.id()).isNotNull();
        assertThat(result.roomId()).isEqualTo(request.roomId());
        assertThat(result.employeeEmail()).isEqualTo(request.employeeEmail());
        assertThat(result.startsAt()).isEqualTo(OffsetDateTime.parse("2030-01-01T10:00:00Z"));
        assertThat(result.startsAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(result.endsAt()).isEqualTo(OffsetDateTime.parse("2030-01-01T11:00:00Z"));
        assertThat(result.endsAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(result.createdAt()).isEqualTo(OffsetDateTime.now(clock));
        var order = inOrder(rooms, validator, bookings);
        order.verify(validator).validate(request.startsAt(), request.endsAt());
        order.verify(rooms).lockById(request.roomId());
        order.verify(validator).validateNotPast(request.startsAt());
        order.verify(bookings).hasOverlap(request.roomId(), request.startsAt(), request.endsAt());
        order.verify(bookings).save(result);
    }

    @Test
    void doesNotPersistBookingForUnknownRoom() {
        assertThatThrownBy(() -> service.create(request, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(validator).validate(request.startsAt(), request.endsAt());
        verifyNoInteractions(bookings);
    }

    @Test
    void doesNotCheckOverlapOrPersistInvalidInterval() {
        var invalid = new InvalidRequestException("Invalid interval");
        doThrow(invalid).when(validator).validate(request.startsAt(), request.endsAt());

        assertThatThrownBy(() -> service.create(request, null)).isSameAs(invalid);
        verifyNoInteractions(rooms, bookings);
    }

    @Test
    void doesNotPersistOverlappingBooking() {
        when(rooms.lockById(request.roomId())).thenReturn(true);
        when(bookings.hasOverlap(request.roomId(), request.startsAt(), request.endsAt())).thenReturn(true);

        assertThatThrownBy(() -> service.create(request, null))
                .isInstanceOf(ConflictException.class);
        verify(bookings, never()).save(any());
    }

    @Test
    void returnsBookingsInRepositoryOrder() {
        Booking first = new Booking(UUID.randomUUID(), request.roomId(), request.employeeEmail(),
                request.startsAt(), request.endsAt(), OffsetDateTime.now(clock));
        Booking second = new Booking(UUID.randomUUID(), "room-2", request.employeeEmail(),
                request.startsAt().plusHours(2), request.endsAt().plusHours(2), OffsetDateTime.now(clock));
        when(bookings.findAllByOrderByStartsAtAscIdAsc(any(Pageable.class)))
                .thenReturn(List.of(first, second));

        var page = service.list(null, 20);
        assertThat(page.content()).containsExactly(first, second);
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void cancelsExistingBooking() {
        UUID id = UUID.randomUUID();
        when(bookings.deleteBookingById(id)).thenReturn(1);

        assertThatCode(() -> service.cancel(id)).doesNotThrowAnyException();
        verify(bookings).deleteBookingById(id);
    }

    @Test
    void reportsMissingBookingOnCancellation() {
        UUID id = UUID.randomUUID();
        when(bookings.deleteBookingById(id)).thenReturn(0);
        assertThatThrownBy(() -> service.cancel(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
