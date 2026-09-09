package com.sber.meetingrooms.controller;

import com.sber.meetingrooms.generated.model.AvailabilityResponse;
import com.sber.meetingrooms.generated.model.BookingPage;
import com.sber.meetingrooms.generated.model.BookingResponse;
import com.sber.meetingrooms.model.Booking;
import com.sber.meetingrooms.model.BookingPageResult;
import com.sber.meetingrooms.model.RoomAvailability;
import org.springframework.stereotype.Component;

@Component
public class ApiModelMapper {
    public BookingResponse toResponse(Booking booking) {
        return new BookingResponse(booking.id(), booking.roomId(), booking.employeeEmail(),
                booking.startsAt(), booking.endsAt(), booking.createdAt());
    }

    public BookingPage toResponse(BookingPageResult page) {
        return new BookingPage(page.content().stream().map(this::toResponse).toList(),
                page.size(), page.nextCursor(), page.hasMore());
    }

    public AvailabilityResponse toResponse(RoomAvailability availability) {
        return new AvailabilityResponse(availability.roomId(), availability.startsAt(),
                availability.endsAt(), availability.available());
    }
}
