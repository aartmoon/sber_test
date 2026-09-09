package com.sber.meetingrooms.model;

import java.time.OffsetDateTime;

public record RoomAvailability(String roomId, OffsetDateTime startsAt,
                               OffsetDateTime endsAt, boolean available) {
}
