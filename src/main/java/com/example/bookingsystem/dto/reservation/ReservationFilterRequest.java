package com.example.bookingsystem.dto.reservation;

import com.example.bookingsystem.entity.ReservationStatus;

import java.math.BigDecimal;

public record ReservationFilterRequest(
        ReservationStatus status,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}