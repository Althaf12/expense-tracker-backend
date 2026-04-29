package com.expensetracker.admin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Admin endpoint to clear all application caches in one shot.
 * Useful for resolving stale-cache issues across all users without restarting the app.
 *
 * POST /api/admin/cache/clear-all
 *
 * Security: covered by the existing permitAll rule for /api/admin/** in SecurityConfig.
 * Restrict or remove this endpoint after maintenance.
 */
@RestController
@RequestMapping("/api/admin/cache")
public class AdminCacheController {

    private static final Logger logger = LoggerFactory.getLogger(AdminCacheController.class);

    private final CacheManager cacheManager;

    public AdminCacheController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Clears every registered cache in the application.
     * Caches cleared: expenseCategories, users, incomes, expenses,
     *                 userExpenseCategories, userExpenses
     */
    @PostMapping("/clear-all")
    public ResponseEntity<?> clearAllCaches() {
        logger.warn("Admin triggered full cache clear");

        Collection<String> cacheNames = cacheManager.getCacheNames();
        List<String> cleared = new ArrayList<>();
        List<String> failed  = new ArrayList<>();

        for (String name : cacheNames) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                try {
                    cache.clear();
                    cleared.add(name);
                    logger.info("Cache '{}' cleared successfully", name);
                } catch (Exception e) {
                    failed.add(name);
                    logger.error("Failed to clear cache '{}'", name, e);
                }
            }
        }

        if (failed.isEmpty()) {
            logger.info("All caches cleared successfully: {}", cleared);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "all caches cleared",
                    "cachesCleared", cleared
            ));
        } else {
            logger.warn("Some caches could not be cleared: {}", failed);
            return ResponseEntity.status(207).body(Map.of(
                    "status", "partial",
                    "message", "some caches could not be cleared",
                    "cachesCleared", cleared,
                    "cachesFailed", failed
            ));
        }
    }
}

