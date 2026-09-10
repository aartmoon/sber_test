package com.sber.meetingrooms.service;

import com.sber.meetingrooms.exception.ConflictException;
import com.sber.meetingrooms.exception.InvalidRequestException;
import com.sber.meetingrooms.exception.ResourceNotFoundException;
import com.sber.meetingrooms.model.Booking;
import com.sber.meetingrooms.repository.BookingRepository;
import com.sber.meetingrooms.repository.RoomRepository;
import com.sber.meetingrooms.validation.BookingIntervalValidator;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BookingCreationService {
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final BookingIntervalValidator intervalValidator;
    private final RequestNormalizer normalizer;
    private final BookingIdempotencyService idempotency;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public BookingCreationService(BookingRepository bookings, RoomRepository rooms,
                                  BookingIntervalValidator intervalValidator,
                                  RequestNormalizer normalizer, BookingIdempotencyService idempotency,
                                  Clock clock,
                                  PlatformTransactionManager transactionManager) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.intervalValidator = intervalValidator;
        this.normalizer = normalizer;
        this.idempotency = idempotency;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Booking create(CreateBookingCommand command, String idempotencyKey) {
        var normalized = normalizer.normalize(command);
        intervalValidator.validateStructure(normalized.startsAt(), normalized.endsAt());
        var idempotencyContext = idempotency.context(normalized, idempotencyKey);
        try {
            intervalValidator.validateNotPast(normalized.startsAt());
        } catch (InvalidRequestException ex) {
            if (!idempotencyContext.enabled()) {
                throw ex;
            }
            return transaction.execute(status -> idempotency.replay(idempotencyContext)
                    .orElseThrow(() -> ex));
        }
        try {
            return transaction.execute(status -> createBooking(normalized, idempotencyContext));
        } catch (DataIntegrityViolationException ex) {
            if (!idempotencyContext.enabled()) {
                throw ex;
            }
            return transaction.execute(status -> idempotency.replay(idempotencyContext)
                    .orElseThrow(() -> ex));
        }
    }

    private Booking createBooking(RequestNormalizer.NormalizedBooking normalized,
                                  BookingIdempotencyService.Context idempotencyContext) {
        var stored = idempotency.replay(idempotencyContext);
        if (stored.isPresent()) {
            return stored.get();
        }
        Booking booking = new Booking(UUID.randomUUID(), normalized.roomId(), normalized.employeeEmail(),
                normalized.startsAt().withOffsetSameInstant(ZoneOffset.UTC),
                normalized.endsAt().withOffsetSameInstant(ZoneOffset.UTC), OffsetDateTime.now(clock));
        idempotency.reserve(idempotencyContext, booking);
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
}
