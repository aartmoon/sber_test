package com.sber.meetingrooms.integration;

import com.sber.meetingrooms.support.ApiIntegrationTestSupport;
import com.sber.meetingrooms.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.flywaydb.core.Flyway;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationIntegrationTest extends ApiIntegrationTestSupport {
    @Autowired Flyway flyway;
    @Autowired RoomRepository rooms;

    @Test
    void appliesAllFlywayMigrations() {
        assertThat(flyway.info().applied()).hasSize(3);
        assertThat(rooms.count()).isEqualTo(3);
    }
}
