package com.sber.meetingrooms.model;

import java.util.List;

public record BookingPageResult(List<Booking> content, int size,
                                String nextCursor, boolean hasMore) {
}
