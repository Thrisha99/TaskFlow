package com.taskflow.dto;

public class AuthResponse {

    private Long id;
    private String name;
    private String email;
    private String message;
    private String token;
    public AuthResponse(
        Long id,
        String name,
        String email,
        String token,
        String message) {

    this.id = id;
    this.name = name;
    this.email = email;
    this.token = token;
    this.message = message;
}
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
public String getToken() {
    return token;
}
    public String getEmail() {
        return email;
    }

    public String getMessage() {
        return message;
    }
}
