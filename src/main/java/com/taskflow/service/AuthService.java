
package com.taskflow.service;

import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.taskflow.dto.AuthResponse;
import com.taskflow.dto.LoginRequest;
import com.taskflow.dto.RegisterRequest;
import com.taskflow.entity.Role;
import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;
import com.taskflow.security.CustomUserDetails;
import com.taskflow.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    // Website and desktop registration: do not issue a JWT.
    public AuthResponse register(RegisterRequest request) {
        return registerUser(request, false);
    }

    // Mobile registration: issue a JWT immediately after successful registration.
    public AuthResponse registerMobile(RegisterRequest request) {
        return registerUser(request, true);
    }

    private AuthResponse registerUser(
            RegisterRequest request,
            boolean issueToken) {

        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        User user = new User();

        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // Never allow a client to assign itself ADMIN.
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);

        String token = null;

        if (issueToken) {
            CustomUserDetails userDetails =
                    new CustomUserDetails(savedUser);

            token = jwtService.generateToken(userDetails);
        }

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                token,
                "Registration successful"
        );
    }

    public AuthResponse login(LoginRequest request) {

        String normalizedEmail = normalizeEmail(request.getEmail());

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                normalizedEmail,
                                request.getPassword()
                        )
                );

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                userDetails.getId(),
                userDetails.getName(),
                userDetails.getUsername(),
                token,
                "Login successful"
        );
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
