package com.sber.meetingrooms.controller;

import com.sber.meetingrooms.generated.api.AvailabilityApi;
import com.sber.meetingrooms.generated.model.AvailabilityResponse;
import com.sber.meetingrooms.service.AvailabilityService;
import java.time.OffsetDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AvailabilityController implements AvailabilityApi {
    private final AvailabilityService service;
    private final ApiModelMapper mapper;

    public AvailabilityController(AvailabilityService service, ApiModelMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<AvailabilityResponse> getAvailability(
            String room, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        return ResponseEntity.ok(mapper.toResponse(service.availability(room, startsAt, endsAt)));
    }
}
