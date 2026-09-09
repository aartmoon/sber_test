package com.sber.meetingrooms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeetingRoomsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeetingRoomsApplication.class, args);
    }
}
