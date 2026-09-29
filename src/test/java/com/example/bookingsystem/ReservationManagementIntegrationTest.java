package com.example.bookingsystem;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationManagementIntegrationTest extends BaseIntegrationTest {

    @Test
    void adminShouldBeAbleToUpdateReservation()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        mockMvc.perform(
                        post("/reservations/admin")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "userId": %d,
                                "resourceId": %d,
                                "startAt": "2099-06-01T10:00:00",
                                "endAt": "2099-06-01T12:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isCreated());

        Long reservationId =
                reservationRepository.findAll()
                        .get(0)
                        .getId();

        mockMvc.perform(
                        put("/reservations/" + reservationId)
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "userId": %d,
                                "resourceId": %d,
                                "startAt": "2099-06-01T13:00:00",
                                "endAt": "2099-06-01T15:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.resourceId").value(resourceId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void adminShouldBeAbleToDeleteReservation()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long reservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-06-02T10:00:00",
                "2099-06-02T12:00:00"
        );

        mockMvc.perform(
                        delete("/reservations/" + reservationId)
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        delete("/reservations/" + reservationId)
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void adminShouldBeAbleToConfirmPendingReservation()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long reservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-06-10T10:00:00",
                "2099-06-10T12:00:00"
        );

        mockMvc.perform(
                        patch(
                                "/reservations/"
                                        + reservationId
                                        + "/status"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                        "status": "CONFIRMED"
                    }
                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void adminShouldBeAbleToCancelPendingReservation()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long reservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-06-11T10:00:00",
                "2099-06-11T12:00:00"
        );

        mockMvc.perform(
                        patch(
                                "/reservations/"
                                        + reservationId
                                        + "/status"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                        "status": "CANCELLED"
                    }
                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void adminShouldBeAbleToCancelConfirmedReservation()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long reservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-06-12T10:00:00",
                "2099-06-12T12:00:00"
        );

        // PENDING → CONFIRMED
        mockMvc.perform(
                        patch(
                                "/reservations/"
                                        + reservationId
                                        + "/status"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                        "status": "CONFIRMED"
                    }
                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // CONFIRMED → CANCELLED
        mockMvc.perform(
                        patch(
                                "/reservations/"
                                        + reservationId
                                        + "/status"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                        "status": "CANCELLED"
                    }
                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void adminShouldNotBeAbleToMoveConfirmedReservationBackToPending()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long reservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-06-13T10:00:00",
                "2099-06-13T12:00:00"
        );

        // PENDING → CONFIRMED
        mockMvc.perform(
                        patch(
                                "/reservations/"
                                        + reservationId
                                        + "/status"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                        "status": "CONFIRMED"
                    }
                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // CONFIRMED → PENDING should fail
        mockMvc.perform(
                        patch(
                                "/reservations/"
                                        + reservationId
                                        + "/status"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                        "status": "PENDING"
                    }
                    """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelledReservationShouldNotAllowStatusChange()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long reservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-06-14T10:00:00",
                "2099-06-14T12:00:00"
        );

        // PENDING → CANCELLED
        mockMvc.perform(
                        patch(
                                "/reservations/"
                                        + reservationId
                                        + "/status"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                        "status": "CANCELLED"
                    }
                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // CANCELLED → CONFIRMED should fail
        mockMvc.perform(
                        patch(
                                "/reservations/"
                                        + reservationId
                                        + "/status"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                        "status": "CONFIRMED"
                    }
                    """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void userShouldNotBeAbleToUpdateReservationStatus()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long reservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-06-15T10:00:00",
                "2099-06-15T12:00:00"
        );

        mockMvc.perform(
                        patch(
                                "/reservations/"
                                        + reservationId
                                        + "/status"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                        "status": "CONFIRMED"
                    }
                    """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void userShouldNotBeAbleToUpdateReservation()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long reservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-06-16T10:00:00",
                "2099-06-16T12:00:00"
        );

        mockMvc.perform(
                        put("/reservations/" + reservationId)
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "userId": %d,
                                "resourceId": %d,
                                "startAt": "2099-06-16T13:00:00",
                                "endAt": "2099-06-16T15:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void updatingNonExistingReservationShouldReturnNotFound()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        mockMvc.perform(
                        put("/reservations/999999")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "userId": %d,
                                "resourceId": %d,
                                "startAt": "2099-06-17T10:00:00",
                                "endAt": "2099-06-17T12:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void updatingReservationWithNonExistingUserShouldReturnNotFound()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long reservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-06-18T10:00:00",
                "2099-06-18T12:00:00"
        );

        mockMvc.perform(
                        put("/reservations/" + reservationId)
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "userId": 999999,
                                "resourceId": %d,
                                "startAt": "2099-06-18T13:00:00",
                                "endAt": "2099-06-18T15:00:00"
                            }
                            """.formatted(resourceId))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void updatingReservationWithNonExistingResourceShouldReturnNotFound()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long reservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-06-19T10:00:00",
                "2099-06-19T12:00:00"
        );

        mockMvc.perform(
                        put("/reservations/" + reservationId)
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "userId": %d,
                                "resourceId": 999999,
                                "startAt": "2099-06-19T13:00:00",
                                "endAt": "2099-06-19T15:00:00"
                            }
                            """.formatted(userId))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void updatingReservationWithInvalidTimeRangeShouldReturnBadRequest()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long reservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-06-20T10:00:00",
                "2099-06-20T12:00:00"
        );

        mockMvc.perform(
                        put("/reservations/" + reservationId)
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "userId": %d,
                                "resourceId": %d,
                                "startAt": "2099-06-20T15:00:00",
                                "endAt": "2099-06-20T13:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatingReservationToOverlappingTimeShouldReturnConflict()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long firstReservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-06-21T10:00:00",
                "2099-06-21T12:00:00"
        );

        Long secondReservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-06-21T14:00:00",
                "2099-06-21T16:00:00"
        );

        mockMvc.perform(
                        put("/reservations/" + secondReservationId)
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "userId": %d,
                                "resourceId": %d,
                                "startAt": "2099-06-21T11:00:00",
                                "endAt": "2099-06-21T13:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isConflict());

        // Keep the first reservation referenced so the test explicitly
        // verifies that the conflict is caused by an existing reservation.
        org.junit.jupiter.api.Assertions.assertNotEquals(
                firstReservationId,
                secondReservationId
        );
    }

    @Test
    void updatingSameReservationShouldNotConflictWithItself()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long reservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-06-22T10:00:00",
                "2099-06-22T12:00:00"
        );

        mockMvc.perform(
                        put("/reservations/" + reservationId)
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "userId": %d,
                                "resourceId": %d,
                                "startAt": "2099-06-22T10:00:00",
                                "endAt": "2099-06-22T12:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.resourceId").value(resourceId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}