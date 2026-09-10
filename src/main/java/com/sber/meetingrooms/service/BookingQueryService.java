package com.sber.meetingrooms.service;

import com.sber.meetingrooms.model.Booking;
import com.sber.meetingrooms.model.BookingPageResult;
import com.sber.meetingrooms.repository.BookingRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingQueryService {
    private final BookingRepository bookings;
    private final BookingCursorCodec cursors;

    public BookingQueryService(BookingRepository bookings, BookingCursorCodec cursors) {
        this.bookings = bookings;
        this.cursors = cursors;
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Booking> listAll() {
        return bookings.findAllByCancelledAtIsNullOrderByStartsAtAscIdAsc();
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public BookingPageResult list(String cursor, int size) {
        var limit = PageRequest.of(0, size + 1);
        List<Booking> result;
        if (cursor == null) {
            result = bookings.findAllByCancelledAtIsNullOrderByStartsAtAscIdAsc(limit);
        } else {
            var position = cursors.decode(cursor);
            result = bookings.findAfter(position.startsAt(), position.id(), limit);
        }

        boolean hasMore = result.size() > size;
        List<Booking> content = hasMore ? result.subList(0, size) : result;
        String nextCursor = hasMore ? cursors.encode(content.getLast()) : null;
        return new BookingPageResult(content, size, nextCursor, hasMore);
    }
}
