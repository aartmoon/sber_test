package com.sber.meetingrooms.integration;

import com.sber.meetingrooms.support.ApiIntegrationTestSupport;
import com.sber.meetingrooms.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationIntegrationTest extends ApiIntegrationTestSupport {
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired RoomRepository rooms;

    @Test
    void usesCursorIndexForBothPages() {
        String first = jdbc.queryForObject("""
                EXPLAIN SELECT * FROM bookings
                ORDER BY starts_at, id FETCH FIRST 21 ROWS ONLY
                """, String.class);
        String next = jdbc.queryForObject("""
                EXPLAIN SELECT * FROM bookings
                WHERE starts_at > ? OR (starts_at = ? AND id > ?)
                ORDER BY starts_at, id FETCH FIRST 21 ROWS ONLY
                """, String.class, java.time.OffsetDateTime.parse("2030-01-01T10:00:00Z"),
                java.time.OffsetDateTime.parse("2030-01-01T10:00:00Z"), java.util.UUID.randomUUID());
        assertThat(first).containsIgnoringCase("idx_bookings_cursor").contains("index sorted");
        assertThat(next).containsIgnoringCase("idx_bookings_cursor").contains("index sorted");
    }

    @Test
    void appliesAllFlywayMigrations() {
        assertThat(flyway.info().applied()).hasSize(4);
        assertThat(rooms.count()).isEqualTo(3);
    }
}
