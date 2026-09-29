package com.example.bookingsystem;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationPaginationAndSortingIntegrationTest extends BaseIntegrationTest {

    @Test
    void adminShouldPaginateAndSortReservations()
            throws Exception {

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long resource = createResource(new BigDecimal("1000.00"));

        createAdminReservation(
                userId,
                resource,
                "2099-12-01T10:00:00",
                "2099-12-01T11:00:00"
        );

        createAdminReservation(
                userId,
                resource,
                "2099-12-02T10:00:00",
                "2099-12-02T11:00:00"
        );

        createAdminReservation(
                userId,
                resource,
                "2099-12-03T10:00:00",
                "2099-12-03T11:00:00"
        );

        mockMvc.perform(
                        get("/reservations")
                                .header("Authorization", "Bearer " + adminToken)
                                .param("page", "0")
                                .param("size", "2")
                                .param("sort", "startAt,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].startAt")
                        .value("2099-12-01T10:00:00"))
                .andExpect(jsonPath("$.content[1].startAt")
                        .value("2099-12-02T10:00:00"));

        mockMvc.perform(
                        get("/reservations")
                                .header("Authorization", "Bearer " + adminToken)
                                .param("page", "1")
                                .param("size", "2")
                                .param("sort", "startAt,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].startAt")
                        .value("2099-12-03T10:00:00"));

        mockMvc.perform(
                        get("/reservations")
                                .header("Authorization", "Bearer " + adminToken)
                                .param("page", "0")
                                .param("size", "2")
                                .param("sort", "startAt,desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].startAt")
                        .value("2099-12-03T10:00:00"))
                .andExpect(jsonPath("$.content[1].startAt")
                        .value("2099-12-02T10:00:00"));
    }

    @Test
    void userShouldPaginateOnlyOwnReservations()
            throws Exception {

        Long userId = userRepository.findByEmail("user@test.com")
                .orElseThrow()
                .getId();

        Long secondUserId = userRepository.findByEmail("seconduser@test.com")
                .orElseThrow()
                .getId();

        Long resource = createResource(new BigDecimal("1000.00"));

        createAdminReservation(
                userId,
                resource,
                "2099-12-10T10:00:00",
                "2099-12-10T11:00:00"
        );

        createAdminReservation(
                userId,
                resource,
                "2099-12-11T10:00:00",
                "2099-12-11T11:00:00"
        );

        createAdminReservation(
                userId,
                resource,
                "2099-12-12T10:00:00",
                "2099-12-12T11:00:00"
        );

        // Another user's reservation must not appear.
        createAdminReservation(
                secondUserId,
                resource,
                "2099-12-13T10:00:00",
                "2099-12-13T11:00:00"
        );

        mockMvc.perform(
                        get("/reservations/my")
                                .header("Authorization", "Bearer " + userToken)
                                .param("page", "0")
                                .param("size", "2")
                                .param("sort", "startAt,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].startAt")
                        .value("2099-12-10T10:00:00"))
                .andExpect(jsonPath("$.content[1].startAt")
                        .value("2099-12-11T10:00:00"));
    }
}