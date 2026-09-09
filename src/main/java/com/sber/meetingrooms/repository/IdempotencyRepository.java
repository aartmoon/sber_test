package com.sber.meetingrooms.repository;

import com.sber.meetingrooms.model.BookingIdempotency;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyRepository extends JpaRepository<BookingIdempotency, String>,
        IdempotencyInsertRepository {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BookingIdempotency request where request.expiresAt <= :now")
    int deleteExpired(@Param("now") OffsetDateTime now);
}
