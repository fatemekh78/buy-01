package com.backend.user_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.dto.Role;
import com.backend.common.exception.CustomException;
import com.backend.common.util.JwtUtil;
import com.backend.user_service.dto.RegisterUserDTO;
import com.backend.user_service.dto.UpdateUserDTO;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserMapper;
import com.backend.user_service.repository.UserRepository;

import jakarta.servlet.http.Cookie;

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
    private InfoUserDTO mockInfoDTO;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id("user123")
                .email("test@example.com")
                .password("hashed_password")
                .role(Role.CLIENT)
                .avatarUrl("http://old-avatar.com/img.jpg")
                .build();

        mockInfoDTO = new InfoUserDTO();
        mockInfoDTO.setEmail("test@example.com");
    }

    // ==========================================
    // REGISTRATION TESTS
    // ==========================================

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
    }

    @Test
    @DisplayName("Should handle Avatar Upload Failure gracefully")
    void registerUser_AvatarUploadFailure() {
        RegisterUserDTO dto = new RegisterUserDTO("John", "Doe", "test@example.com", "password123", Role.CLIENT);
        MultipartFile mockFile = mock(MultipartFile.class);

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userMapper.toEntity(dto)).thenReturn(mockUser);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("hashed_password");
        when(mockFile.isEmpty()).thenReturn(false);

        // Force WebClient to throw an error
        when(webClientBuilder.build()).thenThrow(new RuntimeException("Media Service Down"));

        assertThatThrownBy(() -> userService.registerUser(dto, mockFile))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Could not upload avatar image");
    }

    // ==========================================
    // RETRIEVAL TESTS
    // ==========================================

    @Test
    @DisplayName("Should get user by ID")
    void getUserById_Success() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(userMapper.toInfoUserDTO(mockUser)).thenReturn(mockInfoDTO);

        InfoUserDTO result = userService.getUserById("user123");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when getting user by invalid ID")
    void getUserById_NotFound() {
        when(userRepository.findById("invalid")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUserById("invalid"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Should get user by Email")
    void getUserByEmail_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(userMapper.toInfoUserDTO(mockUser)).thenReturn(mockInfoDTO);

        InfoUserDTO result = userService.getUserByEmail("test@example.com");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    // ==========================================
    // UPDATE TESTS
    // ==========================================

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
    @DisplayName("Should detect email change and flag for new JWT")
    void updateUserInfo_EmailChanged() {
        UpdateUserDTO updateDto = new UpdateUserDTO();
        updateDto.setEmail("new@example.com");

        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        UserService.UserUpdateResult result = userService.updateUserInfo("user123", updateDto);

        assertThat(result.newJwtNeeded()).isTrue();
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("Should throw CONFLICT if new email is already taken")
    void updateUserInfo_EmailChangeConflict() {
        UpdateUserDTO updateDto = new UpdateUserDTO();
        updateDto.setEmail("taken@example.com");

        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUserInfo("user123", updateDto))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Email is already in use");
    }

    @Test
    @DisplayName("Should successfully change password if current password is valid")
    void updateUserInfo_PasswordChangeSuccess() {
        UpdateUserDTO updateDto = new UpdateUserDTO();
        updateDto.setCurrentPassword("correct_password");
        updateDto.setNewPassword("new_secure_password");

        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("correct_password", mockUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("new_secure_password")).thenReturn("new_hashed_password");

        userService.updateUserInfo("user123", updateDto);

        assertThat(mockUser.getPassword()).isEqualTo("new_hashed_password");
        verify(userRepository).save(mockUser);
    }

    // ==========================================
    // DELETION TESTS
    // ==========================================

    @Test
    @DisplayName("Should delete user and publish Kafka event on correct password")
    void deleteUser_Success() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("raw_password", mockUser.getPassword())).thenReturn(true);

        userService.deleteUser("user123", "raw_password");

        verify(kafkaTemplate).send("user-deleted-topic", "user123");
        verify(kafkaTemplate).send("user-avatar-deleted-topic", "http://old-avatar.com/img.jpg");
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
    }

    @Test
    @DisplayName("Should delete avatar and send Kafka event")
    void deleteAvatar_Success() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));

        userService.deleteAvatar("user123");

        verify(kafkaTemplate).send("user-avatar-deleted-topic", "http://old-avatar.com/img.jpg");
        assertThat(mockUser.getAvatarUrl()).isNull();
        verify(userRepository).save(mockUser);
    }

    // ==========================================
    // COOKIE GENERATION TESTS
    // ==========================================

    @Test
    @DisplayName("Should generate JWT Cookie correctly")
    void generateCookie_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateToken(anyMap(), eq("test@example.com"))).thenReturn("mock.jwt.token");

        Cookie result = userService.generateCookie("test@example.com");

        assertThat(result.getName()).isEqualTo("jwt");
        assertThat(result.getValue()).isEqualTo("mock.jwt.token");
        assertThat(result.getMaxAge()).isEqualTo(24 * 60 * 60);
        assertThat(result.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("Should generate empty Cookie for logout")
    void generateEmptyCookie_Success() {
        Cookie result = userService.generateEmptyCookie();

        assertThat(result.getName()).isEqualTo("jwt");
        assertThat(result.getValue()).isNull();
        assertThat(result.getMaxAge()).isEqualTo(0);
    }
}