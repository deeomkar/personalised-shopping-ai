package com.myshop.config;

/** Runtime-only Groq configuration. The API key is read from the environment. */
public record GroqConfig(String apiKey, String model) {
    public static final String DEFAULT_MODEL = "openai/gpt-oss-20b";

    public GroqConfig {
        apiKey = apiKey == null || apiKey.isBlank() ? null : apiKey.trim();
        model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    public static GroqConfig fromEnvironment() {
        return new GroqConfig(System.getenv("GROQ_API_KEY"), System.getenv("GROQ_MODEL"));
    }

    public boolean isConfigured() {
        return apiKey != null;
    }
}
