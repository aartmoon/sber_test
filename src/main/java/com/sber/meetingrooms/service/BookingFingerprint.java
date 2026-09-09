package com.sber.meetingrooms.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class BookingFingerprint {
    public String calculate(RequestNormalizer.NormalizedBooking request) {
        String canonical = String.join("\u0000", request.roomId(), request.employeeEmail(),
                request.startsAt().toInstant().toString(), request.endsAt().toInstant().toString());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
