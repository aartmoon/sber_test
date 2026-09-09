CREATE TABLE booking_idempotency_v2 (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    request_fingerprint VARCHAR(64) NOT NULL,
    booking_id UUID NOT NULL,
    room_id VARCHAR(50) NOT NULL,
    employee_email VARCHAR(254) NOT NULL,
    starts_at TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP(9) WITH TIME ZONE NOT NULL
);

INSERT INTO booking_idempotency_v2(
    idempotency_key, request_fingerprint, booking_id, room_id,
    employee_email, starts_at, ends_at, created_at, expires_at
)
SELECT i.idempotency_key, i.request_fingerprint, b.id, b.room_id,
       b.employee_email, b.starts_at, b.ends_at, b.created_at,
       DATEADD('HOUR', 24, i.created_at)
FROM booking_idempotency i
JOIN bookings b ON b.id = i.booking_id;

DROP TABLE booking_idempotency;
ALTER TABLE booking_idempotency_v2 RENAME TO booking_idempotency;
