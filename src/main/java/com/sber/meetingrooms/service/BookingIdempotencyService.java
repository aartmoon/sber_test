package com.sber.meetingrooms.service;

import com.sber.meetingrooms.config.IdempotencyProperties;
import com.sber.meetingrooms.exception.ConflictException;
import com.sber.meetingrooms.model.Booking;
import com.sber.meetingrooms.model.BookingIdempotency;
import com.sber.meetingrooms.repository.BookingRepository;
import com.sber.meetingrooms.repository.IdempotencyRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class BookingIdempotencyService {
    private final IdempotencyRepository idempotency;
    private final BookingRepository bookings;
    private final RequestNormalizer normalizer;
    private final BookingFingerprint fingerprint;
    private final IdempotencyProperties properties;

    public BookingIdempotencyService(IdempotencyRepository idempotency, BookingRepository bookings,
                                     RequestNormalizer normalizer, BookingFingerprint fingerprint,
                                     IdempotencyProperties properties) {
        this.idempotency = idempotency;
        this.bookings = bookings;
        this.normalizer = normalizer;
        this.fingerprint = fingerprint;
        this.properties = properties;
    }

    public Context context(RequestNormalizer.NormalizedBooking booking, String idempotencyKey) {
        String key = normalizer.normalizeIdempotencyKey(idempotencyKey);
        return new Context(key, key == null ? null : fingerprint.calculate(booking));
    }

    public Optional<Booking> replay(Context context) {
        if (!context.enabled()) {
            return Optional.empty();
        }
        return idempotency.findById(context.key()).map(stored -> {
            if (!stored.fingerprint().equals(context.fingerprint())) {
                throw new ConflictException("Idempotency-Key was already used for a different request");
            }
            Booking booking = stored.booking();
            if (!bookings.existsByIdAndCancelledAtIsNull(booking.id())) {
                throw new ConflictException("The booking created with this Idempotency-Key was cancelled");
            }
            return booking;
        });
    }

    public void reserve(Context context, Booking booking) {
        if (context.enabled()) {
            idempotency.insertAndFlush(new BookingIdempotency(context.key(), context.fingerprint(),
                    booking, booking.createdAt().plus(properties.retention())));
        }
    }

    public record Context(String key, String fingerprint) {
        public boolean enabled() {
            return key != null;
        }
    }
}
