package com.hoanglam.bis.controller;

import com.hoanglam.bis.dto.LoginRequest;
import com.hoanglam.bis.dto.UserResponse;
import com.hoanglam.bis.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hoanglam.bis.dto.response.ApiResponse;

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
}
