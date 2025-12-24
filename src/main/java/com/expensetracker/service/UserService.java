package com.expensetracker.service;

import com.expensetracker.model.User;
import com.expensetracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@CacheConfig(cacheNames = "users")
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Create or update a user. For new users, userId must be provided (from cookie).
     * Sets status to ACTIVE and timestamps appropriately.
     */
    @CacheEvict(allEntries = true)
    public User createOrUpdateUser(User user) {
        if (user.getUserId() == null || user.getUserId().isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }

        Optional<User> existingOpt = userRepository.findById(user.getUserId());
        if (existingOpt.isPresent()) {
            // Update existing user - update lastSeenAt
            User existing = existingOpt.get();
            existing.setLastSeenAt(LocalDateTime.now());
            if (user.getStatus() != null && !user.getStatus().isBlank()) {
                existing.setStatus(user.getStatus());
            }
            logger.info("Updating existing user: {}", user.getUserId());
            return userRepository.save(existing);
        } else {
            // New user
            user.setStatus("ACTIVE");
            user.setCreatedAt(LocalDateTime.now());
            user.setLastSeenAt(LocalDateTime.now());
            logger.info("Creating new user: {}", user.getUserId());
            return userRepository.save(user);
        }
    }

    @Cacheable(key = "#userId")
    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    public boolean existsById(String userId) {
        return userRepository.existsByUserId(userId);
    }

    @CacheEvict(allEntries = true)
    public void deleteUser(String userId) {
        logger.info("Deleting user: {}", userId);
        userRepository.deleteById(userId);
    }

    /**
     * Update last_seen_at timestamp (called on logout)
     */
    @CacheEvict(allEntries = true)
    public User updateLastSeenAt(String userId) {
        Optional<User> opt = userRepository.findById(userId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("user not found");
        }
        User user = opt.get();
        user.setLastSeenAt(LocalDateTime.now());
        logger.info("Updated lastSeenAt for user: {}", userId);
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }
}
