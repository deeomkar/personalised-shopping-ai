package com.myshop.config;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** Runtime-only configuration for the SerpApi Google Shopping adapter. */
public record SerpApiConfig(
        String apiKey,
        URI baseUri,
        String country,
        String language,
        String googleDomain,
        Duration requestTimeout,
        int maxResults
) {
    public static final URI DEFAULT_BASE_URI = URI.create("https://serpapi.com/search.json");
    public static final String DEFAULT_COUNTRY = "in";
    public static final String DEFAULT_LANGUAGE = "en";
    public static final String DEFAULT_GOOGLE_DOMAIN = "google.co.in";
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(15);
    public static final int DEFAULT_MAX_RESULTS = 20;

    public SerpApiConfig {
        apiKey = normalizeApiKey(apiKey);
        baseUri = Objects.requireNonNullElse(baseUri, DEFAULT_BASE_URI);
        country = defaultText(country, DEFAULT_COUNTRY);
        language = defaultText(language, DEFAULT_LANGUAGE);
        googleDomain = defaultText(googleDomain, DEFAULT_GOOGLE_DOMAIN);
        requestTimeout = positive(requestTimeout, "requestTimeout");
        if (maxResults < 1 || maxResults > 20) {
            throw new IllegalArgumentException("maxResults must be between 1 and 20");
        }
    }

    public static SerpApiConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static SerpApiConfig fromEnvironment(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");
        return new SerpApiConfig(
                environment.get("SERPAPI_API_KEY"),
                parseUri(environment.get("SERPAPI_BASE_URL")),
                environment.get("SERPAPI_COUNTRY"),
                environment.get("SERPAPI_LANGUAGE"),
                environment.get("SERPAPI_GOOGLE_DOMAIN"),
                parseTimeout(environment.get("SERPAPI_TIMEOUT_SECONDS")),
                parseInt(environment.get("SERPAPI_MAX_RESULTS"))
        );
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    @Override
    public String toString() {
        return "SerpApiConfig[apiKeyConfigured=" + isConfigured()
                + ", baseUri=" + baseUri
                + ", country=" + country
                + ", language=" + language
                + ", googleDomain=" + googleDomain
                + ", requestTimeout=" + requestTimeout
                + ", maxResults=" + maxResults + "]";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeApiKey(String value) {
        String normalized = normalize(value);
        if (normalized.length() >= 2) {
            char first = normalized.charAt(0);
            char last = normalized.charAt(normalized.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                normalized = normalized.substring(1, normalized.length() - 1).trim();
            }
        }
        if (normalized.chars().anyMatch(character -> Character.isWhitespace(character)
                || character == '"' || character == '\'')) {
            return "";
        }
        return normalized;
    }

    private static String defaultText(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private static URI parseUri(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_BASE_URI;
        }
        try {
            return URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            return DEFAULT_BASE_URI;
        }
    }

    private static Duration parseTimeout(String value) {
        try {
            return value == null || value.isBlank()
                    ? DEFAULT_REQUEST_TIMEOUT
                    : Duration.ofSeconds(Long.parseLong(value.trim()));
        } catch (RuntimeException exception) {
            return DEFAULT_REQUEST_TIMEOUT;
        }
    }

    private static int parseInt(String value) {
        try {
            return value == null || value.isBlank()
                    ? DEFAULT_MAX_RESULTS : Integer.parseInt(value.trim());
        } catch (RuntimeException exception) {
            return DEFAULT_MAX_RESULTS;
        }
    }

    private static Duration positive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
