package com.myshop.config;

/** Runtime-only Gemini configuration. Credentials are read from the process environment. */
public record GeminiConfig(String apiKey, String model) {
    public static final String DEFAULT_MODEL = "gemini-3.8-flash";

    public GeminiConfig {
        apiKey = apiKey == null || apiKey.isBlank() ? null : apiKey.trim();
        model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    public static GeminiConfig fromEnvironment() {
        return new GeminiConfig(System.getenv("GEMINI_API_KEY"), System.getenv("GEMINI_MODEL"));
    }

    public boolean isConfigured() {
        return apiKey != null;
    }
}
