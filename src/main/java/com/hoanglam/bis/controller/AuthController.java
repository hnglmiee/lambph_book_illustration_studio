package com.hoanglam.bis.controller;

import com.hoanglam.bis.dto.LoginRequest;
import com.hoanglam.bis.dto.RegisterRequest;
import com.hoanglam.bis.dto.UserResponse;
import com.hoanglam.bis.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.hoanglam.bis.response.ApiResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        UserResponse user = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Logged in successfully", user));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse result = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công", result));
    }
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }
}
