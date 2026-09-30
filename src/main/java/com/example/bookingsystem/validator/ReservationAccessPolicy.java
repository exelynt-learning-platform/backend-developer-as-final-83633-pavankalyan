package com.example.bookingsystem.validator;

import com.example.bookingsystem.entity.Reservation;
import com.example.bookingsystem.entity.User;
import com.example.bookingsystem.exception.ReservationAccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class ReservationAccessPolicy {

    public void verifyOwnership(Reservation reservation, User user) {
        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new ReservationAccessDeniedException(
                    "You do not have permission to access this reservation"
            );
        }
    }
}