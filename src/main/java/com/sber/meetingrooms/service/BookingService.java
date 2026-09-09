package com.sber.meetingrooms.service;

import com.sber.meetingrooms.exception.ConflictException;
import com.sber.meetingrooms.exception.ResourceNotFoundException;
import com.sber.meetingrooms.model.Booking;
import com.sber.meetingrooms.model.BookingIdempotency;
import com.sber.meetingrooms.model.BookingPageResult;
import com.sber.meetingrooms.repository.BookingRepository;
import com.sber.meetingrooms.repository.IdempotencyRepository;
import com.sber.meetingrooms.repository.RoomRepository;
import com.sber.meetingrooms.validation.BookingIntervalValidator;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private static final Duration IDEMPOTENCY_RETENTION = Duration.ofHours(24);

    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final BookingIntervalValidator intervalValidator;
    private final IdempotencyRepository idempotency;
    private final RequestNormalizer normalizer;
    private final BookingFingerprint fingerprint;
    private final BookingCursorCodec cursors;
    private final Clock clock;

    public BookingService(BookingRepository bookings, RoomRepository rooms,
                          BookingIntervalValidator intervalValidator,
                          IdempotencyRepository idempotency, RequestNormalizer normalizer,
                          BookingFingerprint fingerprint, BookingCursorCodec cursors, Clock clock) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.intervalValidator = intervalValidator;
        this.idempotency = idempotency;
        this.normalizer = normalizer;
        this.fingerprint = fingerprint;
        this.cursors = cursors;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Booking create(CreateBookingCommand command, String idempotencyKey) {
        var normalized = normalizer.normalize(command);
        intervalValidator.validate(normalized.startsAt(), normalized.endsAt());
        String key = normalizer.normalizeIdempotencyKey(idempotencyKey);
        String requestFingerprint = key == null ? null : fingerprint.calculate(normalized);
        if (key != null) {
            idempotency.lock(key);
            var stored = idempotency.findById(key);
            if (stored.isPresent()) {
                if (!stored.get().fingerprint().equals(requestFingerprint)) {
                    throw new ConflictException(
                            "Idempotency-Key was already used for a different request");
                }
                if (!bookings.existsById(stored.get().booking().id())) {
                    throw new ConflictException("The booking created with this Idempotency-Key was cancelled");
                }
                return stored.get().booking();
            }
        }
        if (!rooms.lockById(normalized.roomId())) {
            throw new ResourceNotFoundException("Room not found");
        }
        intervalValidator.validateNotPast(normalized.startsAt());
        if (bookings.hasOverlap(normalized.roomId(), normalized.startsAt(), normalized.endsAt())) {
            throw new ConflictException("Room is already booked for this interval");
        }
        Booking booking = new Booking(UUID.randomUUID(), normalized.roomId(), normalized.employeeEmail(),
                normalized.startsAt().withOffsetSameInstant(ZoneOffset.UTC),
                normalized.endsAt().withOffsetSameInstant(ZoneOffset.UTC), OffsetDateTime.now(clock));
        bookings.save(booking);
        if (key != null) {
            idempotency.save(new BookingIdempotency(key, requestFingerprint, booking,
                    booking.createdAt().plus(IDEMPOTENCY_RETENTION)));
        }
        return booking;
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public BookingPageResult list(String cursor, int size) {
        var limit = PageRequest.of(0, size + 1);
        List<Booking> result;
        if (cursor == null) {
            result = bookings.findAllByOrderByStartsAtAscIdAsc(limit);
        } else {
            var position = cursors.decode(cursor);
            result = bookings.findAfter(position.startsAt(), position.id(), limit);
        }

        boolean hasMore = result.size() > size;
        List<Booking> content = hasMore ? result.subList(0, size) : result;
        String nextCursor = hasMore ? cursors.encode(content.getLast()) : null;
        return new BookingPageResult(content, size, nextCursor, hasMore);
    }

    @Transactional
    public void cancel(UUID id) {
        if (bookings.deleteBookingById(id) == 0) {
            throw new ResourceNotFoundException("Booking not found");
        }
    }
}
