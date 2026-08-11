package com.hoanglam.bis.service;

import com.hoanglam.bis.dto.LoginRequest;
import com.hoanglam.bis.dto.UserResponse;
import com.hoanglam.bis.model.User;
import com.hoanglam.bis.repository.UserRepository;
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
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setName(name);
            user.setCreatedAt(OffsetDateTime.now());
            user = userRepository.save(user);
        }

        return UserResponse.fromEntity(user);
    }
}
