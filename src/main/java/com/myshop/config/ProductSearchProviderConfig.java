package com.myshop.config;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Provider-neutral configuration for the future live product adapter.
 * Provider-specific credentials should be added only after a provider is selected.
 */
public record ProductSearchProviderConfig(
        String providerId,
        String apiKey,
        URI baseUri,
        Duration requestTimeout
) {
    public static final String DEFAULT_PROVIDER_ID = "mock";
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    public ProductSearchProviderConfig {
        providerId = normalizeProviderId(providerId);
        apiKey = apiKey == null ? "" : apiKey.trim();
        if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
    }

    public static ProductSearchProviderConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    /**
     * Kept public so configuration loading can be tested without mutating the process environment.
     */
    public static ProductSearchProviderConfig fromEnvironment(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");
        return new ProductSearchProviderConfig(
                environment.getOrDefault("MYSHOP_PRODUCT_API_PROVIDER", DEFAULT_PROVIDER_ID),
                environment.getOrDefault("MYSHOP_PRODUCT_API_KEY", ""),
                parseUri(environment.get("MYSHOP_PRODUCT_API_BASE_URL")),
                parseTimeout(environment.get("MYSHOP_PRODUCT_API_TIMEOUT_SECONDS"))
        );
    }

    public boolean usesMockProvider() {
        return DEFAULT_PROVIDER_ID.equals(providerId);
    }

    public boolean hasApiKey() {
        return !apiKey.isBlank();
    }

    @Override
    public String toString() {
        return "ProductSearchProviderConfig[providerId=" + providerId
                + ", apiKeyConfigured=" + hasApiKey()
                + ", baseUri=" + baseUri
                + ", requestTimeout=" + requestTimeout + "]";
    }

    private static String normalizeProviderId(String value) {
        String normalized = value == null
                ? DEFAULT_PROVIDER_ID
                : value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? DEFAULT_PROVIDER_ID : normalized;
    }

    private static URI parseUri(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("MYSHOP_PRODUCT_API_BASE_URL must be a valid URI", exception);
        }
    }

    private static Duration parseTimeout(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_REQUEST_TIMEOUT;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(value.trim()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("MYSHOP_PRODUCT_API_TIMEOUT_SECONDS must be an integer", exception);
        }
    }
}
