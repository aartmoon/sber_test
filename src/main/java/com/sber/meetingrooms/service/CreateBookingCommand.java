package com.sber.meetingrooms.service;

import java.time.OffsetDateTime;

public record CreateBookingCommand(
        String roomId,
        String employeeEmail,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt) {
}
