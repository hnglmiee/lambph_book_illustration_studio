package com.hoanglam.bis.config;

import com.hoanglam.bis.model.User;
import com.hoanglam.bis.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    @DisplayName("Should create mock admin user when admin email does not exist in DB")
    void createAdminUserWhenNotPresent() {
        when(userRepository.findByEmail(DataInitializer.ADMIN_EMAIL)).thenReturn(Optional.empty());

        dataInitializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User createdUser = userCaptor.getValue();
        assertThat(createdUser.getId()).isEqualTo(DataInitializer.ADMIN_ID);
        assertThat(createdUser.getEmail()).isEqualTo(DataInitializer.ADMIN_EMAIL);
        assertThat(createdUser.getName()).isEqualTo(DataInitializer.ADMIN_NAME);
    }

    @Test
    @DisplayName("Should skip creating admin user when admin email already exists in DB")
    void skipCreateAdminUserWhenPresent() {
        User existingAdmin = new User();
        existingAdmin.setId(DataInitializer.ADMIN_ID);
        existingAdmin.setEmail(DataInitializer.ADMIN_EMAIL);
        existingAdmin.setName(DataInitializer.ADMIN_NAME);

        when(userRepository.findByEmail(DataInitializer.ADMIN_EMAIL)).thenReturn(Optional.of(existingAdmin));

        dataInitializer.run();

        verify(userRepository, never()).save(any());
    }
}
