package com.stocktrade.matchingsystem.gateway.controller;

import com.stocktrade.matchingsystem.common.model.dto.LoginRequestDTO;
import com.stocktrade.matchingsystem.common.model.dto.RegisterRequestDTO;
import com.stocktrade.matchingsystem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "User authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with loginId/password")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDTO request) {
        return authService.login(request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Invalid credentials")));
    }

    @PostMapping("/register")
    @Operation(summary = "Register with loginId/password")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequestDTO request) {
        return authService.register(request)
                .<ResponseEntity<?>>map(resp -> ResponseEntity.status(201).body(resp))
                .orElseGet(() -> ResponseEntity.badRequest().body(Map.of("message", "Invalid or duplicate account")));
    }
}
