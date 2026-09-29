package com.example.bookingsystem.repository;

import com.example.bookingsystem.entity.Reservation;
import com.example.bookingsystem.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    boolean existsByResourceId(Long resourceId);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Reservation r
            WHERE r.resource.id = :resourceId
              AND r.status IN :statuses
              AND r.startAt < :endAt
              AND r.endAt > :startAt
            """)
    boolean existsOverlappingReservation(
            @Param("resourceId") Long resourceId,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM Reservation r
        WHERE r.resource.id = :resourceId
          AND r.id <> :reservationId
          AND r.status IN :statuses
          AND r.startAt < :endAt
          AND r.endAt > :startAt
        """)
    boolean existsOverlappingReservationExcludingId(
            @Param("resourceId") Long resourceId,
            @Param("reservationId") Long reservationId,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("""
            SELECT r
            FROM Reservation r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:minPrice IS NULL OR r.price >= :minPrice)
              AND (:maxPrice IS NULL OR r.price <= :maxPrice)
            """)
    Page<Reservation> findAllWithFilters(
            @Param("status") ReservationStatus status,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            Pageable pageable
    );

    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.user.id = :userId
              AND (:status IS NULL OR r.status = :status)
              AND (:minPrice IS NULL OR r.price >= :minPrice)
              AND (:maxPrice IS NULL OR r.price <= :maxPrice)
            """)
    Page<Reservation> findByUserWithFilters(
            @Param("userId") Long userId,
            @Param("status") ReservationStatus status,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM Reservation r
        WHERE r.id = :id
        """)
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);
}