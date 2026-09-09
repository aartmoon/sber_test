package com.example.meetingrooms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record BookingRequest(
        @NotBlank @Size(max = 50) String roomId,
        @NotBlank @Email @Size(max = 254) String employeeEmail,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt) {
}
