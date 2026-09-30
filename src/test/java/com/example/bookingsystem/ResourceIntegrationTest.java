package com.example.bookingsystem;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceIntegrationTest extends BaseIntegrationTest {

    @Test
    void userShouldBeAbleToReadResources()
            throws Exception {

        createResource();

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void userShouldNotBeAbleToCreateResource()
            throws Exception {

        mockMvc.perform(
                        post("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "name": "User Resource",
                                    "description": "Should fail",
                                    "price": 1000.00,
                                    "available": true
                                }
                                """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldBeAbleToCreateResource()
            throws Exception {

        mockMvc.perform(
                        post("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "name": "Admin Resource",
                                    "description": "Created by admin",
                                    "price": 1500.00,
                                    "available": true
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Admin Resource"))
                .andExpect(jsonPath("$.price").value(1500.00))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void userShouldNotBeAbleToUpdateResource()
            throws Exception {

        Long resourceId = createResource();

        mockMvc.perform(
                        put("/resources/" + resourceId)
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "name": "Updated",
                                    "description": "Updated",
                                    "price": 2000.00,
                                    "available": true
                                }
                                """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void userShouldNotBeAbleToDeleteResource()
            throws Exception {

        Long resourceId = createResource();

        mockMvc.perform(
                        delete("/resources/" + resourceId)
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldBeAbleToDeleteResourceWithoutReservations()
            throws Exception {

        Long resourceId = createResource();

        mockMvc.perform(
                        delete("/resources/" + resourceId)
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/resources/" + resourceId)
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void adminShouldNotBeAbleToDeleteResourceWithReservations()
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
                                "startAt": "2099-07-01T10:00:00",
                                "endAt": "2099-07-01T12:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        delete("/resources/" + resourceId)
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Resource cannot be deleted because it has existing reservations"
                                )
                );
    }

    @Test
    void adminShouldNotBeAbleToDeleteResourceWithCancelledReservations()
            throws Exception {

        Long resourceId = createResource();

        Long userId = getUserId(USER_EMAIL);

        Long reservationId = createAdminReservation(
                userId,
                resourceId,
                "2099-07-02T10:00:00",
                "2099-07-02T12:00:00"
        );

        mockMvc.perform(
                        patch("/reservations/" + reservationId + "/status")
                                .header(
                                        AUTHORIZATION_HEADER,
                                        adminAuthorization()
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

        mockMvc.perform(
                        delete("/resources/" + resourceId)
                                .header(
                                        AUTHORIZATION_HEADER,
                                        adminAuthorization()
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Resource cannot be deleted because it has existing reservations"
                                )
                );
    }

    @Test
    void adminShouldBeAbleToUpdateResource()
            throws Exception {

        Long resourceId = createResource();

        mockMvc.perform(
                        put("/resources/" + resourceId)
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "name": "Updated Room",
                                    "description": "Updated description",
                                    "price": 2000.00,
                                    "available": true
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Room"))
                .andExpect(jsonPath("$.price").value(2000.00));
    }

    @Test
    void creatingResourceWithTooManyDecimalPlacesShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                        "name": "Invalid Price Resource",
                        "description": "Too many decimal places",
                        "price": 100.123,
                        "available": true
                    }
                    """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void creatingResourceWithTooManyIntegerDigitsShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                        "name": "Invalid Price Resource",
                        "description": "Too many integer digits",
                        "price": 12345678901.00,
                        "available": true
                    }
                    """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void creatingResourceWithValidDecimalPriceShouldSucceed()
            throws Exception {

        mockMvc.perform(
                        post("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                        "name": "Valid Price Resource",
                        "description": "Valid decimal price",
                        "price": 9999999999.99,
                        "available": true
                    }
                    """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(9999999999.99));
    }

    @Test
    void updatingResourceWithTooManyDecimalPlacesShouldReturnBadRequest()
            throws Exception {

        Long resourceId = createResource();

        mockMvc.perform(
                        put("/resources/" + resourceId)
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                        "name": "Updated Resource",
                        "description": "Invalid decimal price",
                        "price": 100.123,
                        "available": true
                    }
                    """)
                )
                .andExpect(status().isBadRequest());
    }
}