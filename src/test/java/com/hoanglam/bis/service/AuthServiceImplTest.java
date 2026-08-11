package com.hoanglam.bis.service;

import com.hoanglam.bis.dto.LoginRequest;
import com.hoanglam.bis.dto.UserResponse;
import com.hoanglam.bis.model.User;
import com.hoanglam.bis.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setEmail("existing@example.com");
        existingUser.setName("Existing User");
        existingUser.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Should return existing user when email already exists in DB")
    void loginExistingUser() {
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        LoginRequest request = LoginRequest.builder()
                .email("existing@example.com")
                .name("Ignored New Name")
                .build();

        UserResponse response = authService.login(request);

        assertThat(response.getId()).isEqualTo(existingUser.getId());
        assertThat(response.getEmail()).isEqualTo("existing@example.com");
        assertThat(response.getName()).isEqualTo("Existing User");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create and save new user when email does not exist in DB")
    void loginNewUser() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginRequest request = LoginRequest.builder()
                .email("new@example.com")
                .name("New User")
                .build();

        UserResponse response = authService.login(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getName()).isEqualTo("New User");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when email is blank")
    void loginBlankEmailThrowsException() {
        LoginRequest request = LoginRequest.builder()
                .email("")
                .name("New User")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email is required");
    }
}
