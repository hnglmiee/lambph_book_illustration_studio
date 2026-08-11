package com.hoanglam.bis.dto;

import com.hoanglam.bis.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String name;
    private String token;
    private OffsetDateTime createdAt;

    public static UserResponse fromEntity(User user, String token) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .createdAt(user.getCreatedAt())
                .token(token)
                .build();
    }
}
