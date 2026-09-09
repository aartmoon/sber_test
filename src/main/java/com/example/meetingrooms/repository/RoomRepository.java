package com.example.meetingrooms.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepository {
    private final JdbcTemplate jdbc;

    public RoomRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existsById(String roomId) {
        return !jdbc.queryForList("SELECT id FROM rooms WHERE id = ?", String.class, roomId).isEmpty();
    }

    // Must run inside the booking transaction: the lock protects overlap checking and insertion.
    public boolean lockById(String roomId) {
        return !jdbc.queryForList("SELECT id FROM rooms WHERE id = ? FOR UPDATE", String.class, roomId).isEmpty();
    }
}
