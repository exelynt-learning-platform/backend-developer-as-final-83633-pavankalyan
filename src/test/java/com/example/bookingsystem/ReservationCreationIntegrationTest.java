package com.example.bookingsystem;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationCreationIntegrationTest extends BaseIntegrationTest {

    @Test
    void userShouldBeAbleToCreateReservation()
            throws Exception {

        Long resourceId = createResource();

        mockMvc.perform(
                        post("/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "resourceId": %d,
                                    "startAt": "2099-01-01T10:00:00",
                                    "endAt": "2099-01-01T12:00:00"
                                }
                                """.formatted(resourceId))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.resourceId").value(resourceId));
    }

    @Test
    void adminShouldNotBeAbleToCreateReservation()
            throws Exception {

        Long resourceId = createResource();

        mockMvc.perform(
                        post("/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "resourceId": %d,
                                    "startAt": "2099-01-02T10:00:00",
                                    "endAt": "2099-01-02T12:00:00"
                                }
                                """.formatted(resourceId))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldBeAbleToCreateReservationForUser()
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
                                "startAt": "2099-05-01T10:00:00",
                                "endAt": "2099-05-01T12:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.resourceId").value(resourceId));
    }

    @Test
    void userShouldNotBeAbleToCreateReservationThroughAdminEndpoint()
            throws Exception {

        Long resourceId = createResource();

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        mockMvc.perform(
                        post("/reservations/admin")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "userId": %d,
                                "resourceId": %d,
                                "startAt": "2099-05-02T10:00:00",
                                "endAt": "2099-05-02T12:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatingReservationForNonExistingUserShouldReturnNotFound()
            throws Exception {

        Long resourceId = createResource();

        mockMvc.perform(
                        post("/reservations/admin")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "userId": 999999,
                                "resourceId": %d,
                                "startAt": "2099-05-03T10:00:00",
                                "endAt": "2099-05-03T12:00:00"
                            }
                            """.formatted(resourceId))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCreatingOverlappingReservationShouldReturnConflict()
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
                                "startAt": "2099-05-04T10:00:00",
                                "endAt": "2099-05-04T12:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isCreated());

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
                                "startAt": "2099-05-04T11:00:00",
                                "endAt": "2099-05-04T13:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void adminCreatingReservationWithInvalidTimeRangeShouldReturnBadRequest()
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
                                "startAt": "2099-05-05T14:00:00",
                                "endAt": "2099-05-05T12:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void reservationWithPastEndTimeShouldReturnBadRequest()
            throws Exception {

        Long resourceId = createResource();

        mockMvc.perform(
                        post("/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "resourceId": %d,
                                "startAt": "2099-05-06T10:00:00",
                                "endAt": "2000-05-06T12:00:00"
                            }
                            """.formatted(resourceId))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void overlappingReservationShouldReturnConflict()
            throws Exception {

        Long resourceId = createResource();

        mockMvc.perform(
                        post("/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "resourceId": %d,
                                "startAt": "2099-08-01T10:00:00",
                                "endAt": "2099-08-01T12:00:00"
                            }
                            """.formatted(resourceId))
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "resourceId": %d,
                                "startAt": "2099-08-01T11:00:00",
                                "endAt": "2099-08-01T13:00:00"
                            }
                            """.formatted(resourceId))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void adjacentReservationShouldBeAllowed()
            throws Exception {

        Long resourceId = createResource();

        mockMvc.perform(
                        post("/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "resourceId": %d,
                                "startAt": "2099-08-02T10:00:00",
                                "endAt": "2099-08-02T12:00:00"
                            }
                            """.formatted(resourceId))
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "resourceId": %d,
                                "startAt": "2099-08-02T12:00:00",
                                "endAt": "2099-08-02T14:00:00"
                            }
                            """.formatted(resourceId))
                )
                .andExpect(status().isCreated());
    }
}