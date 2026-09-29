package com.example.bookingsystem.dto.reservation;

import com.example.bookingsystem.entity.ReservationStatus;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record ReservationFilterRequest(
        ReservationStatus status,

        @DecimalMin(value = "0.00", message = "Minimum price must be greater than or equal to 0")
        BigDecimal minPrice,

        @DecimalMin(value = "0.00", message = "Maximum price must be greater than or equal to 0")
        BigDecimal maxPrice
) {
}