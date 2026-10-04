package com.myshop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.config.GroqConfig;
import com.myshop.model.SearchRequest;
import com.myshop.model.ShoppingIntent;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Groq-backed shopping intent provider using Chat Completions structured outputs. */
public final class GroqShoppingIntentProvider implements ShoppingIntentProvider {
    private static final Logger LOGGER = Logger.getLogger(GroqShoppingIntentProvider.class.getName());
    private final GroqConfig config;
    private final ShoppingIntentProvider fallback;
    private final ShoppingIntentJsonMapper intentMapper;
    private final ObjectMapper objectMapper;
    private final GroqHttpTransport transport;

    public GroqShoppingIntentProvider() {
        this(GroqConfig.fromEnvironment(), new FallbackShoppingIntentProvider());
    }

    public GroqShoppingIntentProvider(GroqConfig config, ShoppingIntentProvider fallback) {
        this(config, fallback, new ShoppingIntentJsonMapper(), new ObjectMapper(), new JavaHttpGroqTransport());
    }

    GroqShoppingIntentProvider(GroqConfig config, ShoppingIntentProvider fallback,
                               ShoppingIntentJsonMapper intentMapper, ObjectMapper objectMapper,
                               GroqHttpTransport transport) {
        this.config = config;
        this.fallback = fallback;
        this.intentMapper = intentMapper;
        this.objectMapper = objectMapper;
        this.transport = transport;
    }

    boolean isConfigured() {
        return config.isConfigured();
    }

    @Override
    public ShoppingIntent understand(SearchRequest request) {
        try {
            return understandLive(request);
        } catch (RuntimeException exception) {
            logFailure(exception);
            return fallback.understand(request);
        }
    }

    ShoppingIntent understandLive(SearchRequest request) {
        if (!config.isConfigured()) {
            throw new IllegalStateException("GROQ_API_KEY is not configured");
        }
        GroqHttpResponse response = transport.post(config.apiKey(), requestBody(request));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new GroqRequestException(response.statusCode(), response.body());
        }
        try {
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Groq response did not contain message content");
            }
            return intentMapper.map(content, request);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalArgumentException("Groq response could not be parsed", exception);
        }
    }

    private String requestBody(SearchRequest request) {
        try {
            var root = objectMapper.createObjectNode();
            root.put("model", config.model());
            root.put("temperature", 0);
            var messages = root.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt());
            messages.addObject().put("role", "user").put("content", userPrompt(request));
            root.set("response_format", responseFormat());
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            throw new IllegalStateException("Groq request could not be created", exception);
        }
    }

    private com.fasterxml.jackson.databind.node.ObjectNode responseFormat() {
        var format = objectMapper.createObjectNode();
        format.put("type", "json_schema");
        var schema = format.putObject("json_schema");
        schema.put("name", "shopping_intent");
        schema.put("strict", true);
        var definition = schema.putObject("schema");
        definition.put("type", "object");
        definition.put("additionalProperties", false);
        var properties = definition.putObject("properties");
        properties.set("category", nullableString());
        properties.set("productType", nullableString());
        properties.set("minBudget", nullableNumber());
        properties.set("maxBudget", nullableNumber());
        for (String field : List.of("preferredBrands", "colors", "useCases", "priorities", "keywords")) {
            var array = properties.putObject(field);
            array.put("type", "array");
            array.putObject("items").put("type", "string");
        }
        properties.set("shoppingStyle", nullableString());
        var required = definition.putArray("required");
        for (String field : List.of("category", "productType", "minBudget", "maxBudget",
                "preferredBrands", "colors", "useCases", "priorities", "keywords", "shoppingStyle")) {
            required.add(field);
        }
        return format;
    }

    private com.fasterxml.jackson.databind.node.ObjectNode nullableString() {
        var value = objectMapper.createObjectNode();
        var types = value.putArray("type");
        types.add("string").add("null");
        return value;
    }

    private com.fasterxml.jackson.databind.node.ObjectNode nullableNumber() {
        var value = objectMapper.createObjectNode();
        var types = value.putArray("type");
        types.add("number").add("null");
        return value;
    }

    private String systemPrompt() {
        return "Extract shopping intent from natural-language shopping queries. Return only the JSON schema. "
                + "Do not invent requirements. The current query always overrides saved preferences. "
                + "Use null for unknown scalar values and empty arrays when there is no evidence. "
                + "Normalize category to Fashion, Footwear, Electronics, Beauty, Accessories, Home, or Sports.";
    }

    private String userPrompt(SearchRequest request) {
        String preferences = request.userPreference() == null
                ? "No saved preferences are available."
                : "Saved preferences for context only: categories=" + request.userPreference().selectedCategories()
                + ", priorities=" + request.userPreference().shoppingPriorities()
                + ", style=" + request.userPreference().shoppingStyle().label()
                + ", favourite brands=" + request.userPreference().favoriteBrands();
        return preferences + "\nCurrent user query: " + request.rawQuery();
    }

    private void logFailure(RuntimeException exception) {
        String message = exception.getMessage() == null ? "unavailable" : exception.getMessage();
        LOGGER.log(Level.WARNING, "Groq intent provider failed; using fallback: "
                + sanitize(message));
    }

    private String sanitize(String value) {
        return value.replace(config.apiKey() == null ? "\u0000" : config.apiKey(), "[REDACTED]")
                .replaceAll("(?i)bearer\\s+[^\\s,;]+", "Bearer [REDACTED]")
                .replaceAll("(?i)(api[-_ ]?key|authorization)\\s*[:=]\\s*[^\\s,;]+", "$1=[REDACTED]");
    }

    static final class GroqRequestException extends RuntimeException {
        private final int statusCode;

        GroqRequestException(int statusCode, String responseBody) {
            super("Groq HTTP " + statusCode + ": " + responseBody);
            this.statusCode = statusCode;
        }

        int statusCode() {
            return statusCode;
        }
    }
}
