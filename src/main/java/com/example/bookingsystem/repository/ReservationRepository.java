package com.example.bookingsystem.repository;

import com.example.bookingsystem.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}