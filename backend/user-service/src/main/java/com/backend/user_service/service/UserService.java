package com.backend.user_service.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.exception.CustomException;
import com.backend.common.util.JwtUtil;
import com.backend.user_service.dto.RegisterUserDTO;
import com.backend.user_service.dto.UpdateUserDTO;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserMapper;
import com.backend.user_service.repository.UserRepository;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core business logic for User identity, authentication, and lifecycle
 * management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
// 1. ADDED UserDetailsService implementation here
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WebClient.Builder webClientBuilder;
    private final UserMapper userMapper;

    /**
     * Internal record to pass multiple return values back to the controller.
     */
    public record UserUpdateResult(boolean newJwtNeeded, String userEmail) {
    }

    // 2. ADDED the loadUserByUsername method to satisfy Spring Security
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found with email: " + email, HttpStatus.NOT_FOUND));

        // Create an authority from the user's role
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(authority));
    }

    @Transactional
    public void registerUser(RegisterUserDTO dto, MultipartFile avatar) {
        log.info("Initiating registration for email: {}", dto.getEmail());

        if (userRepository.existsByEmail(dto.getEmail())) {
            log.warn("Registration failed: Email {} is already in use.", dto.getEmail());
            throw new CustomException("Email is already registered", HttpStatus.CONFLICT);
        }
        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        if (avatar != null && !avatar.isEmpty()) {
            String avatarUrl = uploadAvatar(avatar);
            user.setAvatarUrl(avatarUrl);
        }

        userRepository.save(user);
        log.info("User {} successfully registered with role {}", user.getEmail(), user.getRole());
    }

    public InfoUserDTO getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        return userMapper.toInfoUserDTO(user);
    }

    public InfoUserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        return userMapper.toInfoUserDTO(user);
    }

    @Transactional
    public UserUpdateResult updateUserInfo(String userId, UpdateUserDTO updatedInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        boolean emailChanged = updatedInfo.getEmail() != null && !updatedInfo.getEmail().equals(user.getEmail());

        if (emailChanged && userRepository.existsByEmail(updatedInfo.getEmail())) {
            throw new CustomException("Email is already in use", HttpStatus.CONFLICT);
        }

        // Handle Password Update securely
        if (updatedInfo.getNewPassword() != null) {
            if (updatedInfo.getCurrentPassword() == null ||
                    !passwordEncoder.matches(updatedInfo.getCurrentPassword(), user.getPassword())) {
                throw new CustomException("Invalid current password", HttpStatus.FORBIDDEN);
            }
            user.setPassword(passwordEncoder.encode(updatedInfo.getNewPassword()));
        }

        // Let MapStruct handle the rest of the partial updates
        userMapper.updateUserFromDto(updatedInfo, user);
        userRepository.save(user);

        return new UserUpdateResult(emailChanged, user.getEmail());
    }

    @Transactional
    public void deleteUser(String userId, String rawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("Unauthorized deletion attempt for user ID: {}", userId);
            throw new CustomException("Invalid password", HttpStatus.FORBIDDEN);
        }

        kafkaTemplate.send("user-deleted-topic", userId);
        kafkaSendDeleteAvatar(user.getAvatarUrl());

        userRepository.deleteById(user.getId());
        log.info("User {} permanently deleted.", userId);
    }

    @Transactional
    public void deleteAvatar(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        kafkaSendDeleteAvatar(user.getAvatarUrl());
        user.setAvatarUrl(null);
        userRepository.save(user);
    }

    @Transactional
    public void updateUserAvatar(String userId, MultipartFile avatarFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        // 1. Delete the old avatar via Kafka
        kafkaSendDeleteAvatar(user.getAvatarUrl());

        // 2. Upload the new avatar via the media-service
        String newAvatarUrl = uploadAvatar(avatarFile);
        user.setAvatarUrl(newAvatarUrl);

        // 3. Save the updated user to the database
        userRepository.save(user);
    }

    private void kafkaSendDeleteAvatar(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            kafkaTemplate.send("user-avatar-deleted-topic", avatarUrl);
        }
    }

    /**
     * Uploads the avatar via WebClient to the media-service.
     */
    private String uploadAvatar(MultipartFile avatar) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(avatar.getBytes()) {
                @Override
                public String getFilename() {
                    return avatar.getOriginalFilename();
                }
            });
            String fileUrl = webClientBuilder.build().post()
                    .uri("https://MEDIA-SERVICE/api/media/upload/avatar")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.createException()
                                    .map(ex -> new CustomException("Media service upload failed",
                                            HttpStatus.BAD_GATEWAY)))
                    .bodyToMono(String.class)
                    .block();

            if (fileUrl == null || fileUrl.isBlank()) {
                throw new CustomException("Invalid response from media service", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return fileUrl; // Successfully returns the raw string URL

        } catch (Exception e) {
            log.error("Avatar upload failed: {}", e.getMessage());
            throw new CustomException("Could not upload avatar image", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Cookie generateCookie(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());

        String jwt = jwtUtil.generateToken(claims, user.getEmail());
        return createCookie(jwt, 24 * 60 * 60);
    }

    public Cookie generateEmptyCookie() {
        return createCookie(null, 0);
    }

    private Cookie createCookie(String token, int maxAge) {
        Cookie jwtCookie = new Cookie("jwt", token);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(maxAge);
        // jwtCookie.setHttpOnly(true); // Crucial for preventing XSS attacks
        jwtCookie.setAttribute("SameSite", "Lax");
        return jwtCookie;
    }
}
