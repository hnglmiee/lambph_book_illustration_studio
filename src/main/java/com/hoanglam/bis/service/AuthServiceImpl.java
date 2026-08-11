package com.hoanglam.bis.service;

import com.hoanglam.bis.dto.LoginRequest;
import com.hoanglam.bis.dto.RegisterRequest;
import com.hoanglam.bis.dto.UserResponse;
import com.hoanglam.bis.model.User;
import com.hoanglam.bis.repository.UserRepository;
import com.hoanglam.bis.exceptions.BadRequestException;
import com.hoanglam.bis.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;


    @Override
    @Transactional
    public UserResponse login(LoginRequest request) {

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        String email = request.getEmail().trim().toLowerCase();

        String name = (request.getName() != null && !request.getName().isBlank())
                ? request.getName().trim()
                : "User";

        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setCreatedAt(OffsetDateTime.now());

            user = userRepository.save(user);
        }

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getName()
        );

        return UserResponse.fromEntity(user, token);
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã tồn tại");
        }

        User user = new User();

        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setName(request.getName().trim());
        user.setCreatedAt(OffsetDateTime.now());

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getName()
        );

        return UserResponse.fromEntity(user, token);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BadRequestException("User not found")
                );

        return UserResponse.fromEntity(user, null);
    }
}
