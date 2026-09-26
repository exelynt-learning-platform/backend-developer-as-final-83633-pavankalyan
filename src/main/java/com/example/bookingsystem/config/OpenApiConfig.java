package com.example.bookingsystem.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Resource Booking System API",
                version = "1.0.0",
                description = """
                        RESTful Resource Booking System built with
                        Spring Boot, Spring Security, JWT, JPA, and PostgreSQL.

                        The API supports authentication, resource management,
                        reservations, role-based authorization, reservation
                        conflict detection, filtering, pagination, and sorting.
                        """,
                contact = @Contact(
                        name = "Pavan Kalyan"
                )
        ),
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}