package com.sber.meetingrooms.unit.validation;

import com.sber.meetingrooms.exception.InvalidRequestException;
import com.sber.meetingrooms.validation.BookingIntervalValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class BookingIntervalValidatorTest {
    private final BookingIntervalValidator validator = new BookingIntervalValidator(
            Clock.fixed(Instant.parse("2030-01-01T08:00:00Z"), ZoneOffset.UTC));

    @ParameterizedTest
    @CsvSource({
            "08:00:00,08:15:00", "10:00:00,14:00:00", "10:00:00,11:00:00"
    })
    void acceptsValidDurationAndInclusiveBoundaries(String start, String end) {
        assertThatCode(() -> validator.validate(time(start), time(end))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "07:59:59.999999999,09:00:00",
            "10:00:00,10:14:59.999999999",
            "10:00:00,14:00:00.000000001",
            "10:00:00,10:00:00", "11:00:00,10:00:00"
    })
    void rejectsPastStartAndInvalidDurations(String start, String end) {
        assertThatThrownBy(() -> validator.validate(time(start), time(end)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL,10:00:00", "10:00:00,NULL", "NULL,NULL"}, nullValues = "NULL")
    void requiresBothIntervalEndpoints(String start, String end) {
        assertThatThrownBy(() -> validator.validate(time(start), time(end)))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        error -> assertThat(error.getMessage())
                                .isEqualTo("Both startsAt and endsAt are required"));
    }

    @Test
    void calculatesDurationUsingInstantsAcrossDifferentOffsets() {
        assertThatCode(() -> validator.validate(
                OffsetDateTime.parse("2030-01-01T13:00:00+03:00"),
                OffsetDateTime.parse("2030-01-01T11:00:00Z"))).doesNotThrowAnyException();
    }

    private OffsetDateTime time(String value) {
        return value == null ? null : OffsetDateTime.parse("2030-01-01T" + value + "Z");
    }
}
