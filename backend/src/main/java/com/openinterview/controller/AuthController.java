package com.openinterview.controller;

import com.openinterview.common.Result;
import com.openinterview.dto.auth.AuthResponse;
import com.openinterview.dto.auth.LoginRequest;
import com.openinterview.dto.auth.RefreshTokenRequest;
import com.openinterview.dto.auth.RegisterRequest;
import com.openinterview.service.AuthService;
import com.openinterview.trace.TraceContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request), TraceContext.getTraceId(), "AUTH");
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request), TraceContext.getTraceId(), "AUTH");
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refresh(request), TraceContext.getTraceId(), "AUTH");
    }
}
