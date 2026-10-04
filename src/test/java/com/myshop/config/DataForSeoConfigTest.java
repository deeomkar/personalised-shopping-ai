package com.myshop.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataForSeoConfigTest {

    @Test
    void emptyEnvironmentUsesIndiaDefaultsWithoutCredentials() {
        DataForSeoConfig config = DataForSeoConfig.fromEnvironment(Map.of());

        assertFalse(config.hasCredentials());
        assertEquals("India", config.locationName());
        assertEquals("English", config.languageName());
        assertEquals(DataForSeoConfig.DEFAULT_REQUEST_TIMEOUT, config.requestTimeout());
        assertFalse(config.toString().contains("password"));
    }

    @Test
    void environmentValuesAreLoadedWithoutLeakingSecrets() {
        DataForSeoConfig config = DataForSeoConfig.fromEnvironment(Map.of(
                "DATAFORSEO_LOGIN", "login-value",
                "DATAFORSEO_PASSWORD", "password-value",
                "DATAFORSEO_LOCATION", "India",
                "DATAFORSEO_LANGUAGE", "English",
                "DATAFORSEO_TIMEOUT_SECONDS", "20",
                "DATAFORSEO_POLL_INTERVAL_MILLIS", "0",
                "DATAFORSEO_POLL_TIMEOUT_SECONDS", "2",
                "DATAFORSEO_MAX_RESULTS", "12"
        ));

        assertTrue(config.hasCredentials());
        assertEquals(Duration.ofSeconds(20), config.requestTimeout());
        assertEquals(Duration.ZERO, config.pollInterval());
        assertEquals(Duration.ofSeconds(2), config.pollTimeout());
        assertEquals(12, config.maxResults());
        assertFalse(config.toString().contains("login-value"));
        assertFalse(config.toString().contains("password-value"));
    }
}
