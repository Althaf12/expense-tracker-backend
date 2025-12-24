package com.expensetracker.service;

import com.expensetracker.model.UserPreferences;
import com.expensetracker.repository.UserPreferencesRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserPreferencesService {

    private static final Logger logger = LoggerFactory.getLogger(UserPreferencesService.class);

    private final UserPreferencesRepository userPreferencesRepository;
    private final UserRepository userRepository;

    @Autowired
    public UserPreferencesService(UserPreferencesRepository userPreferencesRepository, UserRepository userRepository) {
        this.userPreferencesRepository = userPreferencesRepository;
        this.userRepository = userRepository;
    }

    public Optional<UserPreferences> findByUserId(String userId) {
        if (userId == null) return Optional.empty();
        return userPreferencesRepository.findByUserId(userId.trim());
    }

    @Transactional
    public UserPreferences createOrUpdatePreferences(UserPreferences prefs) {
        if (prefs == null) throw new IllegalArgumentException("preferences required");
        if (prefs.getUserId() == null || prefs.getUserId().isBlank()) throw new IllegalArgumentException("userId required");

        String userId = prefs.getUserId().trim();

        // Normalize inputs
        String fs = prefs.getFontSize();
        if (fs != null) fs = fs.trim();
        String cc = prefs.getCurrencyCode();
        if (cc != null) cc = cc.trim().toUpperCase();
        String theme = prefs.getTheme();
        if (theme != null) theme = theme.trim();

        Optional<UserPreferences> existingOpt = userPreferencesRepository.findByUserId(userId);
        UserPreferences entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            // Update only provided fields (non-null and non-blank)
            if (fs != null && !fs.isBlank()) {
                if (!(fs.equals("S") || fs.equals("M") || fs.equals("L"))) {
                    throw new IllegalArgumentException("font_size must be one of S, M, L");
                }
                entity.setFontSize(fs);
            }

            if (cc != null && !cc.isBlank()) {
                if (!Constants.VALID_CURRENCY_CODES.contains(cc)) {
                    throw new IllegalArgumentException("invalid currency code");
                }
                entity.setCurrencyCode(cc);
            }

            if (theme != null && !theme.isBlank()) {
                if (!(theme.equals("D") || theme.equals("L"))) {
                    throw new IllegalArgumentException("theme must be one of D or L");
                }
                entity.setTheme(theme);
            }

        } else {
            // Ensure user exists before creating preferences
            if (!userRepository.existsByUserId(userId)) {
                throw new IllegalArgumentException("user does not exist");
            }
            entity = new UserPreferences();
            entity.setUserId(userId);

            if (fs != null && !fs.isBlank()) {
                if (!(fs.equals("S") || fs.equals("M") || fs.equals("L"))) {
                    throw new IllegalArgumentException("font_size must be one of S, M, L");
                }
                entity.setFontSize(fs);
            }

            if (cc != null && !cc.isBlank()) {
                if (!Constants.VALID_CURRENCY_CODES.contains(cc)) {
                    throw new IllegalArgumentException("invalid currency code");
                }
                entity.setCurrencyCode(cc);
            }

            if (theme != null && !theme.isBlank()) {
                if (!(theme.equals("D") || theme.equals("L"))) {
                    throw new IllegalArgumentException("theme must be one of D or L");
                }
                entity.setTheme(theme);
            }
        }

        entity.setLastUpdateTmstp(LocalDateTime.now());
        try {
            logger.info("Saving preferences for userId: {}", userId);
            return userPreferencesRepository.save(entity);
        } catch (DataIntegrityViolationException dive) {
            // Handle race condition
            Optional<UserPreferences> reload = userPreferencesRepository.findByUserId(userId);
            if (reload.isPresent()) {
                UserPreferences existing = reload.get();
                if (fs != null && !fs.isBlank()) existing.setFontSize(fs);
                if (cc != null && !cc.isBlank()) existing.setCurrencyCode(cc);
                if (theme != null && !theme.isBlank()) existing.setTheme(theme);
                existing.setLastUpdateTmstp(LocalDateTime.now());
                return userPreferencesRepository.save(existing);
            }
            throw dive;
        }
    }

    @Transactional
    public UserPreferences createDefaultsForUser(String userId) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId required");
        String u = userId.trim();
        // Ensure user exists before creating defaults
        if (!userRepository.existsByUserId(u)) {
            throw new IllegalArgumentException("user does not exist");
        }
        Optional<UserPreferences> existing = userPreferencesRepository.findByUserId(u);
        if (existing.isPresent()) return existing.get();

        UserPreferences p = new UserPreferences();
        p.setUserId(u);
        p.setFontSize("S");
        p.setCurrencyCode("INR");
        p.setTheme("D");
        p.setLastUpdateTmstp(LocalDateTime.now());
        try {
            logger.info("Creating default preferences for userId: {}", u);
            return userPreferencesRepository.save(p);
        } catch (DataIntegrityViolationException dive) {
            return userPreferencesRepository.findByUserId(u).orElseThrow(() -> dive);
        }
    }
}
