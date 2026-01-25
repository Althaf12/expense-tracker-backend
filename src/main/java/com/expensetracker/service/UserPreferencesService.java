package com.expensetracker.service;

import com.expensetracker.exception.InvalidShowHideInfoException;
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
        String incomeMonth = prefs.getIncomeMonth();
        if (incomeMonth != null) incomeMonth = incomeMonth.trim().toUpperCase();
        String showHideInfo = prefs.getShowHideInfo();
        if (showHideInfo != null) showHideInfo = showHideInfo.trim().toUpperCase();

        Optional<UserPreferences> existingOpt = userPreferencesRepository.findByUserId(userId);
        UserPreferences entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            // Update only provided fields (non-null and non-blank)
            if (fs != null && !fs.isBlank()) {
                if (!Constants.VALID_FONT_SIZES.contains(fs)) {
                    throw new IllegalArgumentException("font_size must be one of " + Constants.VALID_FONT_SIZES);
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
                if (!Constants.VALID_THEMES.contains(theme)) {
                    throw new IllegalArgumentException("theme must be one of " + Constants.VALID_THEMES);
                }
                entity.setTheme(theme);
            }

            if (incomeMonth != null && !incomeMonth.isBlank()) {
                if (!Constants.VALID_INCOME_MONTH_VALUES.contains(incomeMonth)) {
                    throw new IllegalArgumentException("incomeMonth must be one of P or C");
                }
                entity.setIncomeMonth(incomeMonth);
            }

            if (showHideInfo != null && !showHideInfo.isBlank()) {
                if (!Constants.VALID_SHOW_HIDE_INFO_VALUES.contains(showHideInfo)) {
                    throw new InvalidShowHideInfoException(showHideInfo);
                }
                entity.setShowHideInfo(showHideInfo);
            }

        } else {
            // Ensure user exists before creating preferences
            if (!userRepository.existsByUserId(userId)) {
                throw new IllegalArgumentException("user does not exist");
            }
            entity = new UserPreferences();
            entity.setUserId(userId);

            if (fs != null && !fs.isBlank()) {
                if (!Constants.VALID_FONT_SIZES.contains(fs)) {
                    throw new IllegalArgumentException("font_size must be one of " + Constants.VALID_FONT_SIZES);
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
                if (!Constants.VALID_THEMES.contains(theme)) {
                    throw new IllegalArgumentException("theme must be one of " + Constants.VALID_THEMES);
                }
                entity.setTheme(theme);
            }

            if (incomeMonth != null && !incomeMonth.isBlank()) {
                if (!Constants.VALID_INCOME_MONTH_VALUES.contains(incomeMonth)) {
                    throw new IllegalArgumentException("incomeMonth must be one of P or C");
                }
                entity.setIncomeMonth(incomeMonth);
            }

            if (showHideInfo != null && !showHideInfo.isBlank()) {
                if (!Constants.VALID_SHOW_HIDE_INFO_VALUES.contains(showHideInfo)) {
                    throw new InvalidShowHideInfoException(showHideInfo);
                }
                entity.setShowHideInfo(showHideInfo);
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
                if (incomeMonth != null && !incomeMonth.isBlank()) existing.setIncomeMonth(incomeMonth);
                if (showHideInfo != null && !showHideInfo.isBlank()) existing.setShowHideInfo(showHideInfo);
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
        p.setFontSize(Constants.DEFAULT_FONT_SIZE);
        p.setCurrencyCode(Constants.DEFAULT_CURRENCY_CODE);
        p.setTheme(Constants.DEFAULT_THEME);
        p.setIncomeMonth(Constants.DEFAULT_INCOME_MONTH);
        p.setShowHideInfo(Constants.DEFAULT_SHOW_HIDE_INFO);
        p.setLastUpdateTmstp(LocalDateTime.now());
        try {
            logger.info("Creating default preferences for userId: {}", u);
            return userPreferencesRepository.save(p);
        } catch (DataIntegrityViolationException dive) {
            return userPreferencesRepository.findByUserId(u).orElseThrow(() -> dive);
        }
    }
}
