package com.hoanglam.bis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoanglam.bis.dto.LoginRequest;
import com.hoanglam.bis.dto.UserResponse;
import com.hoanglam.bis.service.AuthService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("POST /api/auth/login should return user info and set Set-Cookie header")
    void loginSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse mockResponse = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .name("Test User")
                .createdAt(OffsetDateTime.now())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .name("Test User")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("USER_ID=" + userId)));
    }
}
