CREATE TABLE idempotency_locks (
    bucket INTEGER PRIMARY KEY,
    CONSTRAINT valid_idempotency_bucket CHECK (bucket BETWEEN 0 AND 63)
);

INSERT INTO idempotency_locks(bucket) VALUES
(0), (1), (2), (3), (4), (5), (6), (7),
(8), (9), (10), (11), (12), (13), (14), (15),
(16), (17), (18), (19), (20), (21), (22), (23),
(24), (25), (26), (27), (28), (29), (30), (31),
(32), (33), (34), (35), (36), (37), (38), (39),
(40), (41), (42), (43), (44), (45), (46), (47),
(48), (49), (50), (51), (52), (53), (54), (55),
(56), (57), (58), (59), (60), (61), (62), (63);

CREATE TABLE booking_idempotency (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    request_fingerprint CHAR(64) NOT NULL,
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    created_at TIMESTAMP(9) WITH TIME ZONE NOT NULL
);
