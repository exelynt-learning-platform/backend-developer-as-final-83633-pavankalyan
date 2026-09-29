package com.example.bookingsystem.validator;

import com.example.bookingsystem.entity.ReservationStatus;
import com.example.bookingsystem.exception.InvalidReservationStatusException;
import org.springframework.stereotype.Component;

@Component
public class ReservationStatusValidator {

    public void validateTransition(
            ReservationStatus currentStatus,
            ReservationStatus newStatus
    ) {
        switch (currentStatus) {
            case CANCELLED -> throw new InvalidReservationStatusException(
                    "Cancelled reservations cannot change status"
            );

            case PENDING -> {
                if (newStatus != ReservationStatus.CONFIRMED
                        && newStatus != ReservationStatus.CANCELLED) {
                    throw new InvalidReservationStatusException(
                            "Pending reservation can only be confirmed or cancelled"
                    );
                }
            }

            case CONFIRMED -> {
                if (newStatus != ReservationStatus.CANCELLED) {
                    throw new InvalidReservationStatusException(
                            "Confirmed reservation can only be cancelled"
                    );
                }
            }

            default -> throw new InvalidReservationStatusException(
                    "Unsupported reservation status: " + currentStatus
            );
        }
    }
}