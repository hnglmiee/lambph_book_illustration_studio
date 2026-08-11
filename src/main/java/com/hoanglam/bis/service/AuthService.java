package com.hoanglam.bis.service;

import com.hoanglam.bis.dto.LoginRequest;
import com.hoanglam.bis.dto.RegisterRequest;
import com.hoanglam.bis.dto.UserResponse;

public interface AuthService {
    UserResponse login(LoginRequest request);
    UserResponse register(RegisterRequest request);
}
