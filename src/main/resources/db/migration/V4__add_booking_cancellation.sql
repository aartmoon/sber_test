ALTER TABLE bookings ADD COLUMN cancelled_at TIMESTAMP(9) WITH TIME ZONE;

CREATE INDEX idx_bookings_active_room_interval
    ON bookings(cancelled_at, room_id, starts_at, ends_at);
CREATE INDEX idx_bookings_active_cursor
    ON bookings(cancelled_at, starts_at, id);
