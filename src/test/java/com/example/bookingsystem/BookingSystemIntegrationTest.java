package com.example.bookingsystem;

import com.example.bookingsystem.entity.Resource;
import com.example.bookingsystem.entity.Role;
import com.example.bookingsystem.entity.User;
import com.example.bookingsystem.repository.ResourceRepository;
import com.example.bookingsystem.repository.ReservationRepository;
import com.example.bookingsystem.repository.UserRepository;
import com.example.bookingsystem.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookingSystemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private String userToken;
    private String secondUserToken;
    private String adminToken;

    @BeforeEach
    void setUp() {

        createSecondUser();

        userToken = createToken("user@test.com");
        secondUserToken = createToken("seconduser@test.com");
        adminToken = createToken("admin@test.com");
    }

    private void createSecondUser() {

        if (userRepository.findByEmail("seconduser@test.com").isEmpty()) {

            User secondUser = new User(
                    "seconduser@test.com",
                    passwordEncoder.encode("User@12345"),
                    Role.USER
            );

            userRepository.save(secondUser);
        }
    }

    private String createToken(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow();

        return jwtService.generateToken(user);
    }

    private String createExpiredToken(String email) {

        SecretKey key = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );

        Date now = new Date();
        Date expiredAt = new Date(now.getTime() - 60_000);

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(now.getTime() - 120_000))
                .expiration(expiredAt)
                .signWith(key)
                .compact();
    }

    private String createTokenWithWrongSignature(String email) {

        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "this-is-a-different-test-secret-key-123456"
                        .getBytes(StandardCharsets.UTF_8)
        );

        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + 60_000);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(wrongKey)
                .compact();
    }

    private Long createResource() {
        return createResource(new BigDecimal("1000.00"));
    }

    private Long createResource(BigDecimal price) {
        Resource resource = new Resource(
                "Test Resource",
                "Test resource description",
                price,
                true
        );

        return resourceRepository.save(resource).getId();
    }

    private Long createAdminReservation(
            Long userId,
            Long resourceId,
            String startAt,
            String endAt
    ) throws Exception {

        String response = mockMvc.perform(
                        post("/reservations/admin")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "userId": %d,
                                "resourceId": %d,
                                "startAt": "%s",
                                "endAt": "%s"
                            }
                            """.formatted(
                                        userId,
                                        resourceId,
                                        startAt,
                                        endAt
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("id")
                .asLong();
    }

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
    void adminShouldBeAbleToConfirmPendingReservation()
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
                        "startAt": "2099-06-10T10:00:00",
                        "endAt": "2099-06-10T12:00:00"
                    }
                    """.formatted(userId, resourceId))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        Long reservationId =
                reservationRepository.findAll()
                        .get(0)
                        .getId();

        mockMvc.perform(
                        patch("/reservations/" + reservationId + "/status")
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
                        "startAt": "2099-06-11T10:00:00",
                        "endAt": "2099-06-11T12:00:00"
                    }
                    """.formatted(userId, resourceId))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        Long reservationId =
                reservationRepository.findAll()
                        .get(0)
                        .getId();

        mockMvc.perform(
                        patch("/reservations/" + reservationId + "/status")
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
                        "startAt": "2099-06-12T10:00:00",
                        "endAt": "2099-06-12T12:00:00"
                    }
                    """.formatted(userId, resourceId))
                )
                .andExpect(status().isCreated());

        Long reservationId =
                reservationRepository.findAll()
                        .get(0)
                        .getId();

        // PENDING → CONFIRMED
        mockMvc.perform(
                        patch("/reservations/" + reservationId + "/status")
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
                        patch("/reservations/" + reservationId + "/status")
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
                        "startAt": "2099-06-13T10:00:00",
                        "endAt": "2099-06-13T12:00:00"
                    }
                    """.formatted(userId, resourceId))
                )
                .andExpect(status().isCreated());

        Long reservationId =
                reservationRepository.findAll()
                        .get(0)
                        .getId();

        // PENDING → CONFIRMED
        mockMvc.perform(
                        patch("/reservations/" + reservationId + "/status")
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
                .andExpect(status().isOk());

        // CONFIRMED → PENDING should fail
        mockMvc.perform(
                        patch("/reservations/" + reservationId + "/status")
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
                        "startAt": "2099-06-14T10:00:00",
                        "endAt": "2099-06-14T12:00:00"
                    }
                    """.formatted(userId, resourceId))
                )
                .andExpect(status().isCreated());

        Long reservationId =
                reservationRepository.findAll()
                        .get(0)
                        .getId();

        // PENDING → CANCELLED
        mockMvc.perform(
                        patch("/reservations/" + reservationId + "/status")
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
                .andExpect(status().isOk());

        // CANCELLED → CONFIRMED should fail
        mockMvc.perform(
                        patch("/reservations/" + reservationId + "/status")
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
                        "startAt": "2099-06-15T10:00:00",
                        "endAt": "2099-06-15T12:00:00"
                    }
                    """.formatted(userId, resourceId))
                )
                .andExpect(status().isCreated());

        Long reservationId =
                reservationRepository.findAll()
                        .get(0)
                        .getId();

        mockMvc.perform(
                        patch("/reservations/" + reservationId + "/status")
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
                                "startAt": "2099-06-02T10:00:00",
                                "endAt": "2099-06-02T12:00:00"
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
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "userId": %d,
                                "resourceId": %d,
                                "startAt": "2099-06-02T13:00:00",
                                "endAt": "2099-06-02T15:00:00"
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
                                "startAt": "2099-06-03T10:00:00",
                                "endAt": "2099-06-03T12:00:00"
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
                                "startAt": "2099-06-04T10:00:00",
                                "endAt": "2099-06-04T12:00:00"
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
                                "userId": 999999,
                                "resourceId": %d,
                                "startAt": "2099-06-04T13:00:00",
                                "endAt": "2099-06-04T15:00:00"
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
                                "startAt": "2099-06-05T10:00:00",
                                "endAt": "2099-06-05T12:00:00"
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
                                "resourceId": 999999,
                                "startAt": "2099-06-05T13:00:00",
                                "endAt": "2099-06-05T15:00:00"
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
                                "startAt": "2099-06-06T10:00:00",
                                "endAt": "2099-06-06T12:00:00"
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
                                "startAt": "2099-06-06T14:00:00",
                                "endAt": "2099-06-06T12:00:00"
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
                                "startAt": "2099-06-07T10:00:00",
                                "endAt": "2099-06-07T12:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isCreated());

        Long firstReservationId =
                reservationRepository.findAll()
                        .get(0)
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
                                "startAt": "2099-06-07T14:00:00",
                                "endAt": "2099-06-07T16:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isCreated());

        Long secondReservationId =
                reservationRepository.findAll()
                        .stream()
                        .filter(r -> !r.getId().equals(firstReservationId))
                        .findFirst()
                        .orElseThrow()
                        .getId();

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
                                "startAt": "2099-06-07T11:00:00",
                                "endAt": "2099-06-07T13:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void updatingSameReservationShouldNotConflictWithItself()
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
                                "startAt": "2099-06-08T10:00:00",
                                "endAt": "2099-06-08T12:00:00"
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
                                "startAt": "2099-06-08T11:00:00",
                                "endAt": "2099-06-08T13:00:00"
                            }
                            """.formatted(userId, resourceId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

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
                .andExpect(status().isForbidden());
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
                                    "startAt": "2099-02-01T10:00:00",
                                    "endAt": "2099-02-01T12:00:00"
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
                                    "startAt": "2099-02-01T11:00:00",
                                    "endAt": "2099-02-01T13:00:00"
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
                                    "startAt": "2099-03-01T10:00:00",
                                    "endAt": "2099-03-01T12:00:00"
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
                                    "startAt": "2099-03-01T12:00:00",
                                    "endAt": "2099-03-01T14:00:00"
                                }
                                """.formatted(resourceId))
                )
                .andExpect(status().isCreated());
    }

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
                                .header("Authorization", "Bearer " + adminToken)
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
                                .header("Authorization", "Bearer " + adminToken)
                                .param("minPrice", "1500")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].price").value(2000.00));

        // 3. Maximum price filter
        mockMvc.perform(
                        get("/reservations")
                                .header("Authorization", "Bearer " + adminToken)
                                .param("maxPrice", "800")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].price").value(500.00));

        // 4. Combined minimum + maximum price filter
        mockMvc.perform(
                        get("/reservations")
                                .header("Authorization", "Bearer " + adminToken)
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
                                .header("Authorization", "Bearer " + userToken)
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

        mockMvc.perform(
                        get("/reservations/my")
                                .header("Authorization", "Bearer " + userToken)
                                .param("page", "1")
                                .param("size", "2")
                                .param("sort", "startAt,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].startAt")
                        .value("2099-12-12T10:00:00"));
    }

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

    @Test
    void largePageSizeShouldBeCapped()
            throws Exception {

        createResource();

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .param("size", "1000000")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void resourcePaginationShouldReturnRequestedPageSize()
            throws Exception {

        createResource();
        createResource();
        createResource();

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .param("page", "0")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void resourcePaginationShouldReturnSecondPage()
            throws Exception {

        createResource();
        createResource();
        createResource();

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .param("page", "1")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.numberOfElements").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void requestingPageBeyondAvailableDataShouldReturnEmptyPage()
            throws Exception {

        createResource();
        createResource();

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .param("page", "5")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(5))
                .andExpect(jsonPath("$.numberOfElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void resourcesShouldSupportCustomSorting()
            throws Exception {

        createResource();
        createResource();
        createResource();

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .param("sort", "price,desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sort.sorted").value(true));
    }
}