package org.example.skillforgeapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.skillforgeapi.model.dto.request.LoginRequest;
import org.example.skillforgeapi.model.dto.request.RegisterRequest;
import org.example.skillforgeapi.model.dto.response.JwtResponse;
import org.example.skillforgeapi.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<JwtResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        JwtResponse response = authService.register(registerRequest);
        return ResponseEntity.status(201).body(response);
    }
}
