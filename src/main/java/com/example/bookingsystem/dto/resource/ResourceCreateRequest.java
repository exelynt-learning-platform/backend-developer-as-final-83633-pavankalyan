package com.example.bookingsystem.dto.resource;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ResourceCreateRequest(

        @NotBlank(message = "Resource name is required")
        @Size(max = 150, message = "Resource name must not exceed 150 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", inclusive = true, message = "Price must not be negative")
        BigDecimal price,

        @NotNull(message = "Availability is required")
        Boolean available
) {
}