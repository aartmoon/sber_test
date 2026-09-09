CREATE TABLE booking_idempotency (
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
