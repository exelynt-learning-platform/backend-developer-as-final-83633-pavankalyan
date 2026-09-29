package com.example.bookingsystem;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErrorHandlingIntegrationTest extends BaseIntegrationTest {

    @Test
    void malformedJsonShouldReturnBadRequest()
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
                                    "name": "Broken JSON"
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.message")
                                .value("Malformed request body")
                );
    }

    @Test
    void missingRequestBodyShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        put("/resources/999999")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.message")
                                .value("Request body is required")
                );
    }
}