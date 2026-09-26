package com.example.bookingsystem.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType
) {
}