package com.example.bookingsystem.exception;

public class ReservationAccessDeniedException extends RuntimeException {

    public ReservationAccessDeniedException(String message) {
        super(message);
    }
}