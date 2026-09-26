package com.example.bookingsystem.mapper;

import com.example.bookingsystem.dto.reservation.ReservationResponse;
import com.example.bookingsystem.entity.Reservation;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getResource().getId(),
                reservation.getResource().getName(),
                reservation.getStartAt(),
                reservation.getEndAt(),
                reservation.getStatus(),
                reservation.getPrice()
        );
    }
}