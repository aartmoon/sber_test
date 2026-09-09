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
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate creationTransaction;

    public BookingService(BookingRepository bookings, RoomRepository rooms,
                          BookingIntervalValidator intervalValidator,
                          IdempotencyRepository idempotency, RequestNormalizer normalizer,
                          BookingFingerprint fingerprint, BookingCursorCodec cursors, Clock clock,
                          PlatformTransactionManager transactionManager) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.intervalValidator = intervalValidator;
        this.idempotency = idempotency;
        this.normalizer = normalizer;
        this.fingerprint = fingerprint;
        this.cursors = cursors;
        this.clock = clock;
        this.creationTransaction = new TransactionTemplate(transactionManager);
        this.creationTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.creationTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Booking create(CreateBookingCommand command, String idempotencyKey) {
        var normalized = normalizer.normalize(command);
        intervalValidator.validate(normalized.startsAt(), normalized.endsAt());
        String key = normalizer.normalizeIdempotencyKey(idempotencyKey);
        String requestFingerprint = key == null ? null : fingerprint.calculate(normalized);
        try {
            return creationTransaction.execute(status -> createBooking(normalized, key, requestFingerprint));
        } catch (DataIntegrityViolationException ex) {
            if (key == null) {
                throw ex;
            }
            // The losing insert has rolled back; read the winner in a fresh transaction.
            return creationTransaction.execute(status -> replay(key, requestFingerprint).orElseThrow(() -> ex));
        }
    }

    private Booking createBooking(RequestNormalizer.NormalizedBooking normalized, String key,
                                  String requestFingerprint) {
        if (key != null) {
            var stored = replay(key, requestFingerprint);
            if (stored.isPresent()) {
                return stored.get();
            }
        }
        Booking booking = new Booking(UUID.randomUUID(), normalized.roomId(), normalized.employeeEmail(),
                normalized.startsAt().withOffsetSameInstant(ZoneOffset.UTC),
                normalized.endsAt().withOffsetSameInstant(ZoneOffset.UTC), OffsetDateTime.now(clock));
        if (key != null) {
            idempotency.insertAndFlush(new BookingIdempotency(key, requestFingerprint, booking,
                    booking.createdAt().plus(IDEMPOTENCY_RETENTION)));
        }
        if (!rooms.lockById(normalized.roomId())) {
            throw new ResourceNotFoundException("Room not found");
        }
        intervalValidator.validateNotPast(normalized.startsAt());
        if (bookings.hasOverlap(normalized.roomId(), normalized.startsAt(), normalized.endsAt())) {
            throw new ConflictException("Room is already booked for this interval");
        }
        bookings.save(booking);
        return booking;
    }

    private Optional<Booking> replay(String key, String requestFingerprint) {
        return idempotency.findById(key).map(stored -> {
            if (!stored.fingerprint().equals(requestFingerprint)) {
                throw new ConflictException("Idempotency-Key was already used for a different request");
            }
            Booking booking = stored.booking();
            if (!bookings.existsById(booking.id())) {
                throw new ConflictException("The booking created with this Idempotency-Key was cancelled");
            }
            return booking;
        });
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
