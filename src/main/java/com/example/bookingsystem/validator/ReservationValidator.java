package com.example.bookingsystem.validator;

import com.example.bookingsystem.exception.InvalidReservationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class ReservationValidator {

    public void validateTimeRange(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        LocalDateTime now = LocalDateTime.now();

        if (!startAt.isBefore(endAt)) {
            throw new InvalidReservationException(
                    "Start time must be before end time"
            );
        }

        if (!startAt.isAfter(now)) {
            throw new InvalidReservationException(
                    "Start time must be in the future"
            );
        }

        if (!endAt.isAfter(now)) {
            throw new InvalidReservationException(
                    "End time must be in the future"
            );
        }
    }

    public void validatePriceRange(
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        if (minPrice != null
                && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {

            throw new InvalidReservationException(
                    "Minimum price cannot be greater than maximum price"
            );
        }
    }
}