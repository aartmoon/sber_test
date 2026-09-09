package com.sber.meetingrooms.repository;

import com.sber.meetingrooms.model.BookingIdempotency;

public interface IdempotencyInsertRepository {
    void insertAndFlush(BookingIdempotency request);
}
