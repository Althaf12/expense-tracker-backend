package com.expensetracker.service;

import com.expensetracker.model.UserPreferences;
import com.expensetracker.repository.UserPreferencesRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserPreferencesService {

    private final UserPreferencesRepository userPreferencesRepository;
    private final UserRepository userRepository;

    @Autowired
    public UserPreferencesService(UserPreferencesRepository userPreferencesRepository, UserRepository userRepository) {
        this.userPreferencesRepository = userPreferencesRepository;
        this.userRepository = userRepository;
    }

    public Optional<UserPreferences> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return userPreferencesRepository.findByUsername(username.trim());
    }

    @Transactional
    public UserPreferences createOrUpdatePreferences(UserPreferences prefs) {
        if (prefs == null) throw new IllegalArgumentException("preferences required");
        if (prefs.getUsername() == null || prefs.getUsername().isBlank()) throw new IllegalArgumentException("username required");

        String username = prefs.getUsername().trim();

        // Normalize inputs
        String fs = prefs.getFontSize();
        if (fs != null) fs = fs.trim();
        String cc = prefs.getCurrencyCode();
        if (cc != null) cc = cc.trim().toUpperCase();
        String theme = prefs.getTheme();
        if (theme != null) theme = theme.trim();

        // Try to load existing preferences to avoid overwriting fields with null
        Optional<UserPreferences> existingOpt = userPreferencesRepository.findByUsername(username);
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
            // We're about to create a new preferences record. Ensure the user exists in users table.
            if (userRepository.findByUsername(username).isEmpty()) {
                throw new IllegalArgumentException("user does not exist");
            }
            // No existing prefs: create a new entity and set only provided fields
            entity = new UserPreferences();
            entity.setUsername(username);

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
            return userPreferencesRepository.save(entity);
        } catch (DataIntegrityViolationException dive) {
            // Handle race condition where another transaction created the row after our existence check.
            // Reload existing record and apply updates there.
            Optional<UserPreferences> reload = userPreferencesRepository.findByUsername(username);
            if (reload.isPresent()) {
                UserPreferences existing = reload.get();
                if (fs != null && !fs.isBlank()) existing.setFontSize(fs);
                if (cc != null && !cc.isBlank()) existing.setCurrencyCode(cc);
                if (theme != null && !theme.isBlank()) existing.setTheme(theme);
                existing.setLastUpdateTmstp(LocalDateTime.now());
                return userPreferencesRepository.save(existing);
            }
            throw dive; // rethrow if we can't recover
        }
    }

    @Transactional
    public UserPreferences createDefaultsForUser(String username) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username required");
        String u = username.trim();
        // ensure user exists before creating defaults
        if (userRepository.findByUsername(u).isEmpty()) {
            throw new IllegalArgumentException("user does not exist");
        }
        Optional<UserPreferences> existing = userPreferencesRepository.findByUsername(u);
        if (existing.isPresent()) return existing.get();
        UserPreferences p = new UserPreferences();
        p.setUsername(u);
        p.setFontSize("S");
        p.setCurrencyCode("INR");
        p.setTheme("D");
        p.setLastUpdateTmstp(LocalDateTime.now());
        try {
            return userPreferencesRepository.save(p);
        } catch (DataIntegrityViolationException dive) {
            // Another tx created it concurrently - load and return
            return userPreferencesRepository.findByUsername(u).orElseThrow(() -> dive);
        }
    }
}
