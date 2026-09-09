package com.example.meetingrooms.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Booking(UUID id, String roomId, String employeeEmail,
                      OffsetDateTime startsAt, OffsetDateTime endsAt,
                      OffsetDateTime createdAt) {
}
