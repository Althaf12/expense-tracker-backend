package com.expensetracker.controller;

import com.expensetracker.model.UserPreferences;
import com.expensetracker.service.UserPreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user/preferences")
public class UserPreferencesController {

    private static final Logger logger = LoggerFactory.getLogger(UserPreferencesController.class);

    private final UserPreferencesService userPreferencesService;

    @Autowired
    public UserPreferencesController(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
    }

    @PostMapping("")
    public ResponseEntity<?> upsertPreferences(@RequestBody UserPreferences prefs) {
        try {
            logger.info("Upserting preferences for userId: {}", prefs != null ? prefs.getUserId() : null);
            UserPreferences saved = userPreferencesService.createOrUpdatePreferences(prefs);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException ex) {
            logger.warn("Preference upsert error: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Internal error upserting preferences", ex);
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getPreferences(@PathVariable String userId) {
        logger.info("Getting preferences for userId: {}", userId);
        Optional<UserPreferences> opt = userPreferencesService.findByUserId(userId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "preferences not found"));
        }
        return ResponseEntity.ok(opt.get());
    }
}
