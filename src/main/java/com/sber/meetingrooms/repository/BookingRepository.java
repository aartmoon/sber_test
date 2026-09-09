package com.sber.meetingrooms.repository;

import com.sber.meetingrooms.model.Booking;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    boolean existsByRoomIdAndStartsAtLessThanAndEndsAtGreaterThan(
            String roomId, OffsetDateTime endsAt, OffsetDateTime startsAt);

    default boolean hasOverlap(String roomId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        return existsByRoomIdAndStartsAtLessThanAndEndsAtGreaterThan(roomId, endsAt, startsAt);
    }

    List<Booking> findAllByOrderByStartsAtAscIdAsc(Pageable pageable);

    List<Booking> findAllByOrderByStartsAtAscIdAsc();

    @Query("""
            select booking from Booking booking
            where booking.startsAt > :startsAt
               or (booking.startsAt = :startsAt and booking.id > :id)
            order by booking.startsAt, booking.id
            """)
    List<Booking> findAfter(@Param("startsAt") OffsetDateTime startsAt,
                            @Param("id") UUID id, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Booking booking where booking.id = :id")
    int deleteBookingById(@Param("id") UUID id);
}
