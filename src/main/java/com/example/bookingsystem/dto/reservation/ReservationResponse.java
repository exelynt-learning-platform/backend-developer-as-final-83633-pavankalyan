package com.example.bookingsystem.dto.reservation;

import com.example.bookingsystem.entity.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long userId,
        Long resourceId,
        String resourceName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        ReservationStatus status,
        BigDecimal price
) {
}