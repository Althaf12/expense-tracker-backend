package com.expensetracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    // Comma-separated list of allowed origins/patterns. Can be set via environment variable APP_CORS_ALLOWED_ORIGINS
    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsProperty;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> parsed = parseOrigins(allowedOriginsProperty);

        boolean credentialsAllowed = true;
        if (parsed.isEmpty() || (parsed.size() == 1 && parsed.get(0).equals("*"))) {
            log.warn("CORS allowed-origins is empty or '*'. No origin patterns will be registered; allowCredentials=false to avoid unsafe wildcard.");
            credentialsAllowed = false;
        }

        if (!parsed.isEmpty() && credentialsAllowed) {
            String[] patterns = parsed.toArray(new String[0]);
            log.info("Registering CORS allowedOriginPatterns: {} (allowCredentials=true)", parsed);
            // Register for all paths, not just /api/**
            registry.addMapping("/**")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                    .allowedHeaders("*")
                    .exposedHeaders("Authorization", "Content-Type")
                    .allowedOriginPatterns(patterns)
                    .allowCredentials(true)
                    .maxAge(3600);
        } else {
            log.info("Registering CORS with no allowed origin patterns; requests from other origins will be rejected or denied by the browser.");
            registry.addMapping("/**")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                    .allowedHeaders("*")
                    .exposedHeaders("Authorization", "Content-Type")
                    .allowCredentials(false);
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> patterns = parseOrigins(allowedOriginsProperty);

        if (patterns.isEmpty() || (patterns.size() == 1 && patterns.get(0).equals("*"))) {
            log.warn("CORS allowed-origins is empty or '*'. CorsConfiguration will not allow credentials and no origin patterns set.");
            config.setAllowCredentials(false);
        } else {
            log.info("CorsConfiguration.setAllowedOriginPatterns: {} (allowCredentials=true)", patterns);
            config.setAllowedOriginPatterns(patterns);
            config.setAllowCredentials(true);
        }

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Register for all paths
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Parse comma-separated origins and convert wildcard patterns to Spring-compatible format.
     * Converts "https://*.eternivity.com" to a pattern Spring can match.
     */
    private List<String> parseOrigins(String originsProperty) {
        return Arrays.stream(originsProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
