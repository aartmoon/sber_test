package com.example.meetingrooms.repository;

import com.example.meetingrooms.model.Booking;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingRepository {
    private final JdbcTemplate jdbc;

    public BookingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(Booking booking) {
        jdbc.update("""
                INSERT INTO bookings(id, room_id, employee_email, starts_at, ends_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, booking.id(), booking.roomId(), booking.employeeEmail(),
                booking.startsAt(), booking.endsAt(), booking.createdAt());
    }

    public List<Booking> findAll() {
        return jdbc.query("SELECT * FROM bookings ORDER BY starts_at, id", BookingRepository::mapBooking);
    }

    public boolean deleteById(UUID id) {
        return jdbc.update("DELETE FROM bookings WHERE id = ?", id) > 0;
    }

    public boolean hasOverlap(String roomId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM bookings
                WHERE room_id = ? AND starts_at < ? AND ends_at > ?
                """, Integer.class, roomId, endsAt, startsAt);
        return count != null && count > 0;
    }

    private static Booking mapBooking(ResultSet rs, int rowNum) throws SQLException {
        return new Booking(rs.getObject("id", UUID.class), rs.getString("room_id"),
                rs.getString("employee_email"),
                rs.getObject("starts_at", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                rs.getObject("ends_at", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                rs.getObject("created_at", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC));
    }
}
