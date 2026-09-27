package com.example.bookingsystem.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "reservations",
        indexes = {
                @Index(name = "idx_reservations_user_id", columnList = "user_id"),
                @Index(name = "idx_reservations_resource_id", columnList = "resource_id"),
                @Index(name = "idx_reservations_status", columnList = "status"),
                @Index(
                        name = "idx_reservations_resource_time",
                        columnList = "resource_id,start_at,end_at"
                )
        }
)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    protected Reservation() {
    }

    public Reservation(
            User user,
            Resource resource,
            LocalDateTime startAt,
            LocalDateTime endAt,
            ReservationStatus status,
            BigDecimal price
    ) {
        this.user = user;
        this.resource = resource;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Resource getResource() {
        return resource;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    public void updateDetails(
            User user,
            Resource resource,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BigDecimal price
    ) {
        this.user = user;
        this.resource = resource;
        this.startAt = startAt;
        this.endAt = endAt;
        this.price = price;
    }
}