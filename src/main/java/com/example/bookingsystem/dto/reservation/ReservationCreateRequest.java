package com.example.bookingsystem.dto.reservation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservationCreateRequest(

        @NotNull(message = "Resource ID is required")
        Long resourceId,

        @NotNull(message = "Start time is required")
        @Future(message = "Start time must be in the future")
        LocalDateTime startAt,

        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        LocalDateTime endAt
) {
}