package com.backend.user_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.common.dto.Role;
import com.backend.common.exception.CustomException;
import com.backend.common.util.JwtUtil;
import com.backend.user_service.dto.RegisterUserDTO;
import com.backend.user_service.dto.UpdateUserDTO;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserMapper;
import com.backend.user_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id("user123")
                .email("test@example.com")
                .password("hashed_password")
                .role(Role.CLIENT)
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user without avatar")
    void registerUser_Success() {
        RegisterUserDTO dto = new RegisterUserDTO("John", "Doe", "test@example.com", "password123", Role.CLIENT);

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userMapper.toEntity(dto)).thenReturn(mockUser);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("hashed_password");

        userService.registerUser(dto, null);

        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("Should throw CONFLICT when registering with existing email")
    void registerUser_EmailConflict() {
        RegisterUserDTO dto = new RegisterUserDTO("John", "Doe", "test@example.com", "password123", Role.CLIENT);
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(dto, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update user info successfully without password change")
    void updateUserInfo_Success() {
        UpdateUserDTO updateDto = new UpdateUserDTO();
        updateDto.setFirstName("Johnny");

        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));

        UserService.UserUpdateResult result = userService.updateUserInfo("user123", updateDto);

        assertThat(result.newJwtNeeded()).isFalse();
        verify(userMapper).updateUserFromDto(updateDto, mockUser);
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("Should block password update if current password doesn't match")
    void updateUserInfo_InvalidCurrentPassword() {
        UpdateUserDTO updateDto = new UpdateUserDTO();
        updateDto.setCurrentPassword("wrong_password");
        updateDto.setNewPassword("new_secure_password");

        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrong_password", mockUser.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.updateUserInfo("user123", updateDto))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Invalid current password");
    }

    @Test
    @DisplayName("Should delete user and publish Kafka event on correct password")
    void deleteUser_Success() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("raw_password", mockUser.getPassword())).thenReturn(true);

        userService.deleteUser("user123", "raw_password");

        verify(kafkaTemplate).send("user-deleted-topic", "user123");
        verify(userRepository).deleteById("user123");
    }

    @Test
    @DisplayName("Should block account deletion on incorrect password")
    void deleteUser_WrongPassword() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrong_password", mockUser.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser("user123", "wrong_password"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Invalid password");

        verify(userRepository, never()).deleteById(anyString());
    }
}