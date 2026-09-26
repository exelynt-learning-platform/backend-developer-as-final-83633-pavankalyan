package com.example.bookingsystem.exception;

public class InvalidReservationStatusException extends RuntimeException {

    public InvalidReservationStatusException(String message) {
        super(message);
    }
}