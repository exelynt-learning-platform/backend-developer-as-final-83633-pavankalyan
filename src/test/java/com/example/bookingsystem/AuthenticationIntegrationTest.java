package com.example.bookingsystem;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;

class AuthenticationIntegrationTest extends BaseIntegrationTest {

    @Test
    void loginWithValidCredentialsShouldReturnToken()
            throws Exception {

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "email": "user@test.com",
                                    "password": "User@12345"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginWithInvalidPasswordShouldReturnUnauthorized()
            throws Exception {

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "email": "user@test.com",
                                    "password": "WrongPassword"
                                }
                                """)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutTokenShouldReturnUnauthorized()
            throws Exception {

        mockMvc.perform(
                        get("/resources")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithMalformedTokenShouldReturnUnauthorized()
            throws Exception {

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer this-is-not-a-valid-jwt"
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithInvalidSignatureShouldReturnUnauthorized()
            throws Exception {

        String token =
                createTokenWithWrongSignature("user@test.com");

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithExpiredTokenShouldReturnUnauthorized()
            throws Exception {

        String token =
                createExpiredToken("user@test.com");

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isUnauthorized());
    }
}