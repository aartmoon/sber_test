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
    boolean existsByRoomIdAndStartsAtLessThanAndEndsAtGreaterThanAndCancelledAtIsNull(
            String roomId, OffsetDateTime endsAt, OffsetDateTime startsAt);

    default boolean hasOverlap(String roomId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        return existsByRoomIdAndStartsAtLessThanAndEndsAtGreaterThanAndCancelledAtIsNull(
                roomId, endsAt, startsAt);
    }

    boolean existsByIdAndCancelledAtIsNull(UUID id);

    List<Booking> findAllByCancelledAtIsNullOrderByStartsAtAscIdAsc(Pageable pageable);

    List<Booking> findAllByCancelledAtIsNullOrderByStartsAtAscIdAsc();

    @Query("""
            select booking from Booking booking
            where booking.cancelledAt is null
              and (booking.startsAt > :startsAt
                or (booking.startsAt = :startsAt and booking.id > :id))
            order by booking.startsAt, booking.id
            """)
    List<Booking> findAfter(@Param("startsAt") OffsetDateTime startsAt,
                            @Param("id") UUID id, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Booking booking
            set booking.cancelledAt = :cancelledAt
            where booking.id = :id and booking.cancelledAt is null
            """)
    int cancelById(@Param("id") UUID id, @Param("cancelledAt") OffsetDateTime cancelledAt);
}
