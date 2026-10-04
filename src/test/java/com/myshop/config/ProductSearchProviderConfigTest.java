package com.myshop.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductSearchProviderConfigTest {

    @Test
    void emptyEnvironmentKeepsMockAsTheSafeDefault() {
        ProductSearchProviderConfig config = ProductSearchProviderConfig.fromEnvironment(Map.of());

        assertTrue(config.usesMockProvider());
        assertFalse(config.hasApiKey());
        assertEquals(Duration.ofSeconds(10), config.requestTimeout());
    }

    @Test
    void environmentValuesConfigureProviderWithoutLeakingTheApiKeyInToString() {
        ProductSearchProviderConfig config = ProductSearchProviderConfig.fromEnvironment(Map.of(
                "MYSHOP_PRODUCT_API_PROVIDER", "DataForSEO",
                "MYSHOP_PRODUCT_API_KEY", "secret-value",
                "MYSHOP_PRODUCT_API_BASE_URL", "https://api.example.com",
                "MYSHOP_PRODUCT_API_TIMEOUT_SECONDS", "20"
        ));

        assertEquals("dataforseo", config.providerId());
        assertTrue(config.hasApiKey());
        assertEquals("https://api.example.com", config.baseUri().toString());
        assertEquals(Duration.ofSeconds(20), config.requestTimeout());
        assertFalse(config.toString().contains("secret-value"));
    }
}
