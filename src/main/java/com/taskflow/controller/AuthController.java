
package com.taskflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskflow.dto.ApiResponse;
import com.taskflow.dto.AuthResponse;
import com.taskflow.dto.LoginRequest;
import com.taskflow.dto.RegisterRequest;
import com.taskflow.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Website and Windows desktop registration.
    // The user must log in separately after registration.
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Registration successful. Please log in.",
                        response
                ));
    }

    // Mobile registration.
    // A JWT is returned immediately after successful registration.
    @PostMapping("/register/mobile")
    public ResponseEntity<ApiResponse<AuthResponse>> registerMobile(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.registerMobile(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Registration successful. You are now logged in.",
                        response
                ));
    }

    // Login for website, desktop and mobile clients.
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Login successful",
                        response
                )
        );
    }
}
