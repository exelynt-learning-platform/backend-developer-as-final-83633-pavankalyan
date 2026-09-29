package com.example.bookingsystem;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationAccessIntegrationTest extends BaseIntegrationTest {

    @Test
    void adminShouldBeAbleToReadAllReservations()
            throws Exception {

        mockMvc.perform(
                        get("/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void userShouldNotBeAbleToReadAllReservations()
            throws Exception {

        mockMvc.perform(
                        get("/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void userShouldBeAbleToReadOwnReservations()
            throws Exception {

        mockMvc.perform(
                        get("/reservations/my")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void userShouldNotBeAbleToViewAnotherUsersReservation()
            throws Exception {

        Long resourceId = createResource();

        String response =
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
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        Long reservationId =
                objectMapper
                        .readTree(response)
                        .get("id")
                        .asLong();

        mockMvc.perform(
                        get("/reservations/my/" + reservationId)
                                .header(
                                        "Authorization",
                                        "Bearer " + secondUserToken
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("You do not have permission to access this reservation"));
    }
}