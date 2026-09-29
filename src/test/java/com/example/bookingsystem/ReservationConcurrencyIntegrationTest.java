package com.example.bookingsystem;

import com.example.bookingsystem.dto.reservation.ReservationCreateRequest;
import com.example.bookingsystem.entity.ReservationStatus;
import com.example.bookingsystem.entity.User;
import com.example.bookingsystem.entity.Resource;
import com.example.bookingsystem.entity.Role;
import com.example.bookingsystem.exception.ReservationConflictException;
import com.example.bookingsystem.repository.ReservationRepository;
import com.example.bookingsystem.repository.ResourceRepository;
import com.example.bookingsystem.repository.UserRepository;
import com.example.bookingsystem.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:reservation_concurrency_test;DB_CLOSE_DELAY=-1"
})
class ReservationConcurrencyIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        createUserIfNotExists(
                "concurrent-user-1@test.com",
                "ConcurrentUser1@123"
        );

        createUserIfNotExists(
                "concurrent-user-2@test.com",
                "ConcurrentUser2@123"
        );
    }

    @Test
    void shouldAllowOnlyOneReservationWhenTwoUsersBookSameSlotConcurrently()
            throws Exception {

        Long resourceId = createResource();

        User firstUser = userRepository
                .findByEmail("concurrent-user-1@test.com")
                .orElseThrow();

        User secondUser = userRepository
                .findByEmail("concurrent-user-2@test.com")
                .orElseThrow();

        LocalDateTime startAt =
                LocalDateTime.now().plusHours(2);

        LocalDateTime endAt =
                startAt.plusHours(1);

        ReservationCreateRequest request =
                new ReservationCreateRequest(
                        resourceId,
                        startAt,
                        endAt
                );

        CountDownLatch startLatch = new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<?> firstAttempt = executor.submit(() -> {
                awaitStart(startLatch);

                reservationService.create(
                        request,
                        firstUser.getEmail()
                );
            });

            Future<?> secondAttempt = executor.submit(() -> {
                awaitStart(startLatch);

                reservationService.create(
                        request,
                        secondUser.getEmail()
                );
            });

            startLatch.countDown();

            int successfulReservations = 0;
            int conflictFailures = 0;

            for (Future<?> attempt :
                    List.of(firstAttempt, secondAttempt)) {

                try {
                    attempt.get();
                    successfulReservations++;
                } catch (Exception exception) {
                    Throwable cause = exception.getCause();

                    if (cause instanceof ReservationConflictException) {
                        conflictFailures++;
                    } else {
                        throw exception;
                    }
                }
            }

            assertEquals(
                    1,
                    successfulReservations,
                    "Exactly one concurrent reservation should succeed"
            );

            assertEquals(
                    1,
                    conflictFailures,
                    "Exactly one concurrent reservation should fail with a conflict"
            );

            long activeReservationCount =
                    reservationRepository.findAll()
                            .stream()
                            .filter(reservation ->
                                    reservation.getResource()
                                            .getId()
                                            .equals(resourceId)
                            )
                            .filter(reservation ->
                                    reservation.getStatus() == ReservationStatus.PENDING
                                            || reservation.getStatus() == ReservationStatus.CONFIRMED
                            )
                            .count();

            assertEquals(
                    1,
                    activeReservationCount,
                    "Only one active reservation should exist for the resource"
            );

        } finally {
            executor.shutdownNow();
        }
    }

    private void awaitStart(CountDownLatch startLatch) {
        try {
            startLatch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Concurrent test thread was interrupted",
                    exception
            );
        }
    }

    private Long createResource() {
        Resource resource =
                new Resource(
                        "Concurrent Test Resource",
                        "Resource used for concurrency testing",
                        new BigDecimal("1000.00"),
                        true
                );

        return resourceRepository.save(resource).getId();
    }

    private void createUserIfNotExists(
            String email,
            String password
    ) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user =
                    new User(
                            email,
                            passwordEncoder.encode(password),
                            Role.USER
                    );

            userRepository.save(user);
        }
    }
}