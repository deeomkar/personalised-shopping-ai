package com.myshop.config;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** Runtime-only configuration for the DataForSEO Google Shopping adapter. */
public record DataForSeoConfig(
        String login,
        String password,
        URI baseUri,
        String locationName,
        String languageName,
        Duration requestTimeout,
        Duration pollInterval,
        Duration pollTimeout,
        int maxResults
) {
    public static final URI DEFAULT_BASE_URI = URI.create("https://api.dataforseo.com");
    public static final String DEFAULT_LOCATION = "India";
    public static final String DEFAULT_LANGUAGE = "English";
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(15);
    public static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(500);
    public static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofSeconds(12);
    public static final int DEFAULT_MAX_RESULTS = 20;

    public DataForSeoConfig {
        login = normalize(login);
        password = normalize(password);
        baseUri = Objects.requireNonNullElse(baseUri, DEFAULT_BASE_URI);
        locationName = defaultText(locationName, DEFAULT_LOCATION);
        languageName = defaultText(languageName, DEFAULT_LANGUAGE);
        requestTimeout = positive(requestTimeout, "requestTimeout");
        pollInterval = nonNegative(pollInterval, "pollInterval");
        pollTimeout = positive(pollTimeout, "pollTimeout");
        if (maxResults < 1 || maxResults > 20) {
            throw new IllegalArgumentException("maxResults must be between 1 and 20");
        }
    }

    public static DataForSeoConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static DataForSeoConfig fromEnvironment(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");
        return new DataForSeoConfig(
                environment.get("DATAFORSEO_LOGIN"),
                environment.get("DATAFORSEO_PASSWORD"),
                parseUri(environment.get("DATAFORSEO_BASE_URL")),
                environment.get("DATAFORSEO_LOCATION"),
                environment.get("DATAFORSEO_LANGUAGE"),
                parseDurationSeconds(environment.get("DATAFORSEO_TIMEOUT_SECONDS"), DEFAULT_REQUEST_TIMEOUT),
                parseDurationMillis(environment.get("DATAFORSEO_POLL_INTERVAL_MILLIS"), DEFAULT_POLL_INTERVAL),
                parseDurationSeconds(environment.get("DATAFORSEO_POLL_TIMEOUT_SECONDS"), DEFAULT_POLL_TIMEOUT),
                parseInt(environment.get("DATAFORSEO_MAX_RESULTS"), DEFAULT_MAX_RESULTS)
        );
    }

    public boolean hasCredentials() {
        return !login.isBlank() && !password.isBlank();
    }

    @Override
    public String toString() {
        return "DataForSeoConfig[credentialsConfigured=" + hasCredentials()
                + ", baseUri=" + baseUri
                + ", locationName=" + locationName
                + ", languageName=" + languageName
                + ", requestTimeout=" + requestTimeout
                + ", pollInterval=" + pollInterval
                + ", pollTimeout=" + pollTimeout
                + ", maxResults=" + maxResults + "]";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
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

    private static Duration parseDurationSeconds(String value, Duration fallback) {
        try {
            return value == null || value.isBlank()
                    ? fallback : Duration.ofSeconds(Long.parseLong(value.trim()));
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static Duration parseDurationMillis(String value, Duration fallback) {
        try {
            return value == null || value.isBlank()
                    ? fallback : Duration.ofMillis(Long.parseLong(value.trim()));
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static Duration positive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static Duration nonNegative(Duration value, String field) {
        if (value == null || value.isNegative()) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }
}
