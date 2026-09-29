package com.example.bookingsystem.service;

import com.example.bookingsystem.dto.auth.LoginRequest;
import com.example.bookingsystem.dto.auth.LoginResponse;
import com.example.bookingsystem.entity.User;
import com.example.bookingsystem.repository.UserRepository;
import com.example.bookingsystem.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new IllegalStateException("Authenticated user not found")
                );

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer"
        );
    }

    public void logout(String email) {
        int updatedRows =
                userRepository.incrementTokenVersionByEmail(email);

        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "Authenticated user not found"
            );
        }
    }
}