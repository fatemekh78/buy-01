package com.backend.user_service.repository;

import com.backend.user_service.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for User documents.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Locates a user by their exact, unique email address.
     *
     * @param email The email address to search for.
     * @return An Optional containing the User if found.
     */
    Optional<User> findByEmail(String email);

    /**
     * High-performance boolean check to verify if an email is already registered.
     * Used during the registration flow to prevent duplication errors.
     *
     * @param email The email address to check.
     * @return True if a user with this email exists, false otherwise.
     */
    boolean existsByEmail(String email);
}