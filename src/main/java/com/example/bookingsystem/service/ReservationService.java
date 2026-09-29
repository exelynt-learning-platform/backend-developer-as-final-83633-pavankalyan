package com.example.bookingsystem.service;

import com.example.bookingsystem.dto.reservation.AdminReservationCreateRequest;
import com.example.bookingsystem.dto.reservation.ReservationCreateRequest;
import com.example.bookingsystem.dto.reservation.ReservationFilterRequest;
import com.example.bookingsystem.dto.reservation.ReservationResponse;
import com.example.bookingsystem.entity.Reservation;
import com.example.bookingsystem.entity.ReservationStatus;
import com.example.bookingsystem.entity.Resource;
import com.example.bookingsystem.entity.User;
import com.example.bookingsystem.exception.*;
import com.example.bookingsystem.mapper.ReservationMapper;
import com.example.bookingsystem.repository.ReservationRepository;
import com.example.bookingsystem.repository.ResourceRepository;
import com.example.bookingsystem.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ReservationMapper reservationMapper;

    public ReservationService(
            ReservationRepository reservationRepository,
            ResourceRepository resourceRepository,
            UserRepository userRepository,
            ReservationMapper reservationMapper
    ) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.reservationMapper = reservationMapper;
    }

    public ReservationResponse create(
            ReservationCreateRequest request,
            String userEmail
    ) {

        User user = findUserByEmail(userEmail);

        return createReservation(
                user,
                request.resourceId(),
                request.startAt(),
                request.endAt()
        );
    }

    public ReservationResponse createByAdmin(
            AdminReservationCreateRequest request
    ) {

        User user = findUserById(request.userId());

        return createReservation(
                user,
                request.resourceId(),
                request.startAt(),
                request.endAt()
        );
    }

    public ReservationResponse updateByAdmin(
            Long reservationId,
            AdminReservationCreateRequest request
    ) {

        Resource resource = resourceRepository.findByIdForUpdate(request.resourceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resource not found with id: " + request.resourceId()
                        )
                );

        Reservation reservation =
                reservationRepository.findByIdForUpdate(reservationId)
                        .orElseThrow(() ->
                                new ReservationNotFoundException(
                                        "Reservation not found with id: "
                                                + reservationId
                                )
                        );

        User user = findUserById(request.userId());

        assertSlotAvailable(
                resource,
                request.startAt(),
                request.endAt(),
                reservationId
        );

        reservation.updateDetails(
                user,
                resource,
                request.startAt(),
                request.endAt(),
                resource.getPrice()
        );

        return reservationMapper.toResponse(reservation);
    }

    private ReservationResponse createReservation(
            User user,
            Long resourceId,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {

        Resource resource = resourceRepository.findByIdForUpdate(resourceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resource not found with id: " + resourceId
                        )
                );

        assertSlotAvailable(
                resource,
                startAt,
                endAt,
                null
        );

        Reservation reservation = new Reservation(
                user,
                resource,
                startAt,
                endAt,
                ReservationStatus.PENDING,
                resource.getPrice()
        );

        Reservation savedReservation =
                reservationRepository.save(reservation);

        return reservationMapper.toResponse(savedReservation);
    }

    private void assertSlotAvailable(
            Resource resource,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Long excludedReservationId
    ) {

        validateTimeRange(startAt, endAt);

        if (!resource.isAvailable()) {
            throw new InvalidReservationException(
                    "Resource is currently unavailable"
            );
        }

        boolean conflict =
                reservationRepository.existsOverlappingReservation(
                        resource.getId(),
                        excludedReservationId,
                        List.of(
                                ReservationStatus.PENDING,
                                ReservationStatus.CONFIRMED
                        ),
                        startAt,
                        endAt
                );

        if (conflict) {
            throw new ReservationConflictException(
                    "Resource is already reserved for the requested time"
            );
        }
    }

    public Page<ReservationResponse> getOwnReservations(
            String userEmail,
            ReservationFilterRequest filters,
            Pageable pageable
    ) {

        User user = findUserByEmail(userEmail);

        validatePriceRange(
                filters.minPrice(),
                filters.maxPrice()
        );

        return reservationRepository.findByUserWithFilters(
                user.getId(),
                filters.status(),
                filters.minPrice(),
                filters.maxPrice(),
                pageable
        ).map(reservationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getOwnReservation(
            Long reservationId,
            String userEmail
    ) {

        User user = findUserByEmail(userEmail);

        Reservation reservation = findReservation(reservationId);

        verifyOwnership(reservation, user);

        return reservationMapper.toResponse(reservation);
    }

    public Page<ReservationResponse> getAllReservations(
            ReservationFilterRequest filters,
            Pageable pageable
    ){

        validatePriceRange(
                filters.minPrice(),
                filters.maxPrice()
        );

        return reservationRepository.findAllWithFilters(
                filters.status(),
                filters.minPrice(),
                filters.maxPrice(),
                pageable
        ).map(reservationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(
            Long reservationId
    ) {

        return reservationMapper.toResponse(
                findReservation(reservationId)
        );
    }

    public ReservationResponse updateStatus(
            Long reservationId,
            ReservationStatus newStatus
    ) {

        Reservation reservation = findReservation(reservationId);

        if (reservation.getStatus() == newStatus) {
            return reservationMapper.toResponse(reservation);
        }

        validateStatusTransition(
                reservation.getStatus(),
                newStatus
        );

        reservation.changeStatus(newStatus);

        return reservationMapper.toResponse(reservation);
    }

    public void delete(Long reservationId) {

        Reservation reservation = findReservation(reservationId);

        reservationRepository.delete(reservation);
    }

    private User findUserByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found"
                        )
                );
    }

    private User findUserById(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with id: " + userId
                        )
                );
    }

    private Reservation findReservation(Long id) {

        return reservationRepository.findById(id)
                .orElseThrow(() ->
                        new ReservationNotFoundException(
                                "Reservation not found with id: " + id
                        )
                );
    }

    private void verifyOwnership(
            Reservation reservation,
            User user
    ) {

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new ReservationAccessDeniedException(
                    "You do not have permission to access this reservation"
            );
        }
    }

    private void validateTimeRange(
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

    private void validatePriceRange(
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

        if (minPrice != null && minPrice.signum() < 0) {
            throw new InvalidReservationException(
                    "Minimum price cannot be negative"
            );
        }

        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new InvalidReservationException(
                    "Maximum price cannot be negative"
            );
        }
    }

    private void validateStatusTransition(
            ReservationStatus currentStatus,
            ReservationStatus newStatus
    ) {
        switch (currentStatus) {
            case CANCELLED -> throw new InvalidReservationStatusException(
                    "Cancelled reservations cannot change status"
            );

            case PENDING -> {
                if (newStatus != ReservationStatus.CONFIRMED
                        && newStatus != ReservationStatus.CANCELLED) {
                    throw new InvalidReservationStatusException(
                            "Pending reservation can only be confirmed or cancelled"
                    );
                }
            }

            case CONFIRMED -> {
                if (newStatus != ReservationStatus.CANCELLED) {
                    throw new InvalidReservationStatusException(
                            "Confirmed reservation can only be cancelled"
                    );
                }
            }

            default -> throw new InvalidReservationStatusException(
                    "Unsupported reservation status: " + currentStatus
            );
        }
    }
}