package com.example.meetingrooms.dto;

import java.time.OffsetDateTime;

public record Availability(String roomId, OffsetDateTime startsAt,
                           OffsetDateTime endsAt, boolean available) {
}
