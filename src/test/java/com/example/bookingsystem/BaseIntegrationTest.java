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
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    protected static final String USER_EMAIL = "user@test.com";
    protected static final String ADMIN_EMAIL = "admin@test.com";
    protected static final String SECOND_USER_EMAIL = "seconduser@test.com";

    protected static final String AUTHORIZATION_HEADER = "Authorization";
    protected static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ResourceRepository resourceRepository;

    @Autowired
    protected ReservationRepository reservationRepository;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${app.jwt.secret}")
    protected String jwtSecret;

    protected String userToken;
    protected String secondUserToken;
    protected String adminToken;

    @BeforeEach
    void setUp() {

        createSecondUser();

        userToken = createToken(USER_EMAIL);
        secondUserToken = createToken(SECOND_USER_EMAIL);
        adminToken = createToken(ADMIN_EMAIL);
    }

    protected void createSecondUser() {

        if (userRepository.findByEmail(SECOND_USER_EMAIL).isEmpty()) {

            User secondUser = new User(
                    SECOND_USER_EMAIL,
                    passwordEncoder.encode("User@12345"),
                    Role.USER
            );

            userRepository.save(secondUser);
        }
    }

    protected String createToken(String email) {

        var user = userRepository.findByEmail(email)
                .orElseThrow();

        return jwtService.generateToken(user);
    }

    protected Long getUserId(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow()
                .getId();
    }

    protected String userAuthorization() {

        return BEARER_PREFIX + userToken;
    }

    protected String secondUserAuthorization() {

        return BEARER_PREFIX + secondUserToken;
    }

    protected String adminAuthorization() {

        return BEARER_PREFIX + adminToken;
    }

    protected String createExpiredToken(String email) {

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

    protected String createTokenWithWrongSignature(String email) {

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

    protected Long createResource() {

        return createResource(new BigDecimal("1000.00"));
    }

    protected Long createResource(BigDecimal price) {

        Resource resource = new Resource(
                "Test Resource",
                "Test resource description",
                price,
                true
        );

        return resourceRepository
                .save(resource)
                .getId();
    }

    protected Long createAdminReservation(
            Long userId,
            Long resourceId,
            String startAt,
            String endAt
    ) throws Exception {

        String response = mockMvc.perform(
                        post("/reservations/admin")
                                .header(
                                        AUTHORIZATION_HEADER,
                                        adminAuthorization()
                                )
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
}