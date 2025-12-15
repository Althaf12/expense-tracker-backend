package com.expensetracker.controller;

import com.expensetracker.model.UserPreferences;
import com.expensetracker.service.UserPreferencesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user/preferences")
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    @Autowired
    public UserPreferencesController(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
    }

    @PostMapping("")
    public ResponseEntity<?> upsertPreferences(@RequestBody UserPreferences prefs) {
        try {
            UserPreferences saved = userPreferencesService.createOrUpdatePreferences(prefs);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getPreferences(@PathVariable String username) {
        Optional<UserPreferences> opt = userPreferencesService.findByUsername(username);
        if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "preferences not found"));
        return ResponseEntity.ok(opt.get());
    }
}

