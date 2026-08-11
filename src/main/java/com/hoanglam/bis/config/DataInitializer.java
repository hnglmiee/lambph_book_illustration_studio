package com.hoanglam.bis.config;

import com.hoanglam.bis.model.User;
import com.hoanglam.bis.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    public static final String ADMIN_EMAIL = "admin@bookstudio.com";
    public static final String ADMIN_NAME = "System Admin";

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            User admin = new User();
            admin.setEmail(ADMIN_EMAIL);
            admin.setName(ADMIN_NAME);
            admin.setCreatedAt(OffsetDateTime.now());

            userRepository.save(admin);
            log.info("Successfully initialized mock admin user with email: {} and ID: {}", ADMIN_EMAIL);
        } else {
            log.info("Mock admin user already exists with email: {}", ADMIN_EMAIL);
        }
    }
}
