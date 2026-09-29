package com.example.bookingsystem.controller;

import com.example.bookingsystem.dto.reservation.AdminReservationCreateRequest;
import com.example.bookingsystem.dto.reservation.ReservationCreateRequest;
import com.example.bookingsystem.dto.reservation.ReservationResponse;
import com.example.bookingsystem.dto.reservation.ReservationStatusUpdateRequest;
import com.example.bookingsystem.entity.ReservationStatus;
import com.example.bookingsystem.service.ReservationService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.bookingsystem.dto.reservation.ReservationFilterRequest;

import java.math.BigDecimal;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(
            ReservationService reservationService
    ) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationCreateRequest request,
            Authentication authentication
    ) {

        ReservationResponse response =
                reservationService.create(
                        request,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationResponse> createByAdmin(
            @Valid @RequestBody AdminReservationCreateRequest request
    ) {

        ReservationResponse response =
                reservationService.createByAdmin(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<ReservationResponse>> getMyReservations(
            Authentication authentication,

            @ParameterObject
            ReservationFilterRequest filters,

            @ParameterObject
            @PageableDefault(
                    size = 10,
                    sort = "startAt"
            )
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                reservationService.getOwnReservations(
                        authentication.getName(),
                        filters,
                        pageable
                )
        );
    }

    @GetMapping("/my/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReservationResponse> getMyReservation(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                reservationService.getOwnReservation(
                        id,
                        authentication.getName()
                )
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReservationResponse>> getAllReservations(
            @ParameterObject
            ReservationFilterRequest filters,

            @ParameterObject
            @PageableDefault(
                    size = 10,
                    sort = "startAt"
            )
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                reservationService.getAllReservations(
                        filters,
                        pageable
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationResponse> getReservation(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                reservationService.getReservationById(id)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody AdminReservationCreateRequest request
    ) {

        return ResponseEntity.ok(
                reservationService.updateByAdmin(id, request)
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReservationStatusUpdateRequest request
    ) {

        return ResponseEntity.ok(
                reservationService.updateStatus(
                        id,
                        request.status()
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        reservationService.delete(id);

        return ResponseEntity.noContent().build();
    }
}