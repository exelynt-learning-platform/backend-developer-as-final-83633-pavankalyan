package com.example.bookingsystem;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationFilteringIntegrationTest extends BaseIntegrationTest {

    @Test
    void invalidReservationTimeRangeShouldReturnBadRequest()
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
                                    "startAt": "2099-04-01T14:00:00",
                                    "endAt": "2099-04-01T12:00:00"
                                }
                                """.formatted(resourceId))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidPriceRangeShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        get("/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .param("minPrice", "2000")
                                .param("maxPrice", "1000")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidReservationStatusParameterShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        get("/reservations/my")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .param("status", "INVALID")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.message")
                                .value("Invalid value for parameter 'status'")
                );
    }

    @Test
    void invalidSortPropertyShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .param("sort", "doesNotExist")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.message")
                                .value("Invalid sort property")
                );
    }

    @Test
    void adminShouldFilterReservationsByStatusAndPriceRange()
            throws Exception {

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long resource500 = createResource(new BigDecimal("500.00"));
        Long resource1000 = createResource(new BigDecimal("1000.00"));
        Long resource2000 = createResource(new BigDecimal("2000.00"));

        // PENDING reservation - price 500
        createAdminReservation(
                userId,
                resource500,
                "2099-10-01T10:00:00",
                "2099-10-01T11:00:00"
        );

        // CONFIRMED reservation - price 2000
        Long confirmedReservationId = createAdminReservation(
                userId,
                resource2000,
                "2099-10-02T10:00:00",
                "2099-10-02T11:00:00"
        );

        mockMvc.perform(
                patch("/reservations/" + confirmedReservationId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "status": "CONFIRMED"
                            }
                            """)
        ).andExpect(status().isOk());

        // PENDING reservation - price 1000
        createAdminReservation(
                userId,
                resource1000,
                "2099-10-03T10:00:00",
                "2099-10-03T11:00:00"
        );

        // 1. Status filter
        mockMvc.perform(
                        get("/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .param("status", "CONFIRMED")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(confirmedReservationId))
                .andExpect(jsonPath("$.content[0].status")
                        .value("CONFIRMED"));

        // 2. Minimum price filter
        mockMvc.perform(
                        get("/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .param("minPrice", "1500")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].price").value(2000.00));

        // 3. Maximum price filter
        mockMvc.perform(
                        get("/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .param("maxPrice", "800")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].price").value(500.00));

        // 4. Combined minimum + maximum price filter
        mockMvc.perform(
                        get("/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .param("minPrice", "800")
                                .param("maxPrice", "1500")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].price").value(1000.00));
    }

    @Test
    void userShouldFilterOwnReservationsByStatusAndPrice()
            throws Exception {

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long secondUserId = userRepository.findByEmail("seconduser@test.com")
                .orElseThrow()
                .getId();

        Long resource2000 = createResource(new BigDecimal("2000.00"));

        // Reservation belonging to the logged-in USER
        Long ownReservationId = createAdminReservation(
                userId,
                resource2000,
                "2099-11-01T10:00:00",
                "2099-11-01T11:00:00"
        );

        // Reservation belonging to another USER
        Long otherUserReservationId = createAdminReservation(
                secondUserId,
                resource2000,
                "2099-11-02T10:00:00",
                "2099-11-02T11:00:00"
        );

        // Confirm both reservations
        mockMvc.perform(
                patch("/reservations/" + ownReservationId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "status": "CONFIRMED"
                            }
                            """)
        ).andExpect(status().isOk());

        mockMvc.perform(
                patch("/reservations/" + otherUserReservationId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "status": "CONFIRMED"
                            }
                            """)
        ).andExpect(status().isOk());

        // USER should see only their own matching reservation
        mockMvc.perform(
                        get("/reservations/my")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .param("status", "CONFIRMED")
                                .param("minPrice", "1500")
                                .param("maxPrice", "2500")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(ownReservationId))
                .andExpect(jsonPath("$.content[0].status")
                        .value("CONFIRMED"))
                .andExpect(jsonPath("$.content[0].price")
                        .value(2000.00));
    }
}