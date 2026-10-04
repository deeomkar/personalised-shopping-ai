package com.myshop.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerpApiConfigTest {

    @Test
    void emptyEnvironmentUsesIndiaDefaultsWithoutKey() {
        SerpApiConfig config = SerpApiConfig.fromEnvironment(Map.of());

        assertFalse(config.isConfigured());
        assertEquals("in", config.country());
        assertEquals("en", config.language());
        assertEquals("google.co.in", config.googleDomain());
        assertEquals(SerpApiConfig.DEFAULT_REQUEST_TIMEOUT, config.requestTimeout());
    }

    @Test
    void environmentValuesAreLoadedWithoutLeakingTheKey() {
        SerpApiConfig config = SerpApiConfig.fromEnvironment(Map.of(
                "SERPAPI_API_KEY", "secret-value",
                "SERPAPI_COUNTRY", "in",
                "SERPAPI_LANGUAGE", "en",
                "SERPAPI_GOOGLE_DOMAIN", "google.co.in",
                "SERPAPI_TIMEOUT_SECONDS", "20",
                "SERPAPI_MAX_RESULTS", "12"
        ));

        assertTrue(config.isConfigured());
        assertEquals(Duration.ofSeconds(20), config.requestTimeout());
        assertEquals(12, config.maxResults());
        assertFalse(config.toString().contains("secret-value"));
    }

    @Test
    void stripsOptionalQuoteDelimitersWithoutPuttingQuotesIntoRequests() {
        SerpApiConfig config = new SerpApiConfig(
                "  \"test-key\"  ", null, null, null, null,
                SerpApiConfig.DEFAULT_REQUEST_TIMEOUT, SerpApiConfig.DEFAULT_MAX_RESULTS
        );

        assertEquals("test-key", config.apiKey());
        assertTrue(config.isConfigured());
    }

    @Test
    void rejectsKeysContainingWhitespaceInsteadOfSendingA401ProneValue() {
        SerpApiConfig config = new SerpApiConfig(
                "test key", null, null, null, null,
                SerpApiConfig.DEFAULT_REQUEST_TIMEOUT, SerpApiConfig.DEFAULT_MAX_RESULTS
        );

        assertFalse(config.isConfigured());
    }
}
