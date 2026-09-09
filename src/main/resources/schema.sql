CREATE TABLE rooms (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);
CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    room_id VARCHAR(50) NOT NULL REFERENCES rooms(id),
    employee_email VARCHAR(254) NOT NULL,
    starts_at TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    CONSTRAINT valid_interval CHECK (ends_at > starts_at)
);
CREATE INDEX idx_bookings_room_interval ON bookings(room_id, starts_at, ends_at);
