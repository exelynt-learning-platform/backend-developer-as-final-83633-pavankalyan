package com.example.bookingsystem.dto.reservation;

import com.example.bookingsystem.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record ReservationStatusUpdateRequest(

        @NotNull(message = "Reservation status is required")
        ReservationStatus status
) {
}