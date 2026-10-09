package com.myshop.service;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import com.myshop.config.GeminiConfig;
import com.myshop.model.SearchRequest;
import com.myshop.model.ShoppingIntent;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Gemini-backed intent provider with a silent deterministic fallback. */
public final class GeminiShoppingIntentProvider implements ShoppingIntentProvider {
    private static final Logger LOGGER = Logger.getLogger(GeminiShoppingIntentProvider.class.getName());
    private static final Schema STRING_LIST = Schema.builder()
            .type(Type.Known.ARRAY)
            .items(Schema.builder().type(Type.Known.STRING))
            .build();
    private static final Schema INTENT_SCHEMA = Schema.builder()
            .type(Type.Known.OBJECT)
            .properties(Map.ofEntries(
                    Map.entry("category", optionalString()),
                    Map.entry("productType", optionalString()),
                    Map.entry("minBudget", optionalNumber()),
                    Map.entry("maxBudget", optionalNumber()),
                    Map.entry("preferredBrands", STRING_LIST),
                    Map.entry("colors", STRING_LIST),
                    Map.entry("useCases", STRING_LIST),
                    Map.entry("priorities", STRING_LIST),
                    Map.entry("keywords", STRING_LIST),
                    Map.entry("shoppingStyle", optionalString()),
                    Map.entry("qualityPreference", optionalString())
            ))
            .required("category", "productType", "minBudget", "maxBudget", "preferredBrands",
                    "colors", "useCases", "priorities", "keywords", "shoppingStyle", "qualityPreference")
            .build();

    private final GeminiConfig config;
    private final ShoppingIntentProvider fallback;
    private final ShoppingIntentJsonMapper mapper;

    public GeminiShoppingIntentProvider() {
        this(GeminiConfig.fromEnvironment(), new FallbackShoppingIntentProvider());
    }

    public GeminiShoppingIntentProvider(GeminiConfig config, ShoppingIntentProvider fallback) {
        this(config, fallback, new ShoppingIntentJsonMapper());
    }

    GeminiShoppingIntentProvider(GeminiConfig config, ShoppingIntentProvider fallback,
                                 ShoppingIntentJsonMapper mapper) {
        this.config = config;
        this.fallback = fallback;
        this.mapper = mapper;
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
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }
        Client client = Client.builder().apiKey(config.apiKey()).build();
        String responseText = client.models.generateContent(
                config.model(), prompt(request), GenerateContentConfig.builder()
                        .candidateCount(1)
                        .temperature(0F)
                        .responseMimeType("application/json")
                        .responseSchema(INTENT_SCHEMA)
                        .build()
        ).text();
        if (responseText == null || responseText.isBlank()) {
            throw new IllegalArgumentException("Gemini returned an empty response");
        }
        return mapper.map(responseText, request);
    }

    private void logFailure(RuntimeException exception) {
        StringBuilder detail = new StringBuilder("Gemini intent provider failed; using fallback")
                .append(" exception=").append(exception.getClass().getSimpleName());
        if (exception instanceof ApiException apiException) {
            detail.append(" httpStatus=").append(apiException.code())
                    .append(" status=").append(sanitize(apiException.status()))
                    .append(" message=").append(sanitize(apiException.message()));
        } else {
            detail.append(" message=").append(sanitize(exception.getMessage()));
        }
        LOGGER.log(Level.WARNING, detail.toString());
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unavailable";
        }
        return value
                .replace(config.apiKey() == null ? "\u0000" : config.apiKey(), "[REDACTED]")
                .replaceAll("(?i)bearer\\s+[^\\s,;]+", "Bearer [REDACTED]")
                .replaceAll("(?i)(api[-_ ]?key|authorization)\\s*[:=]\\s*[^\\s,;]+", "$1=[REDACTED]");
    }

    private String prompt(SearchRequest request) {
        String preferences = request.userPreference() == null
                ? "No saved preferences are available."
                : "Saved preferences (use only when the query is ambiguous): "
                + "categories=" + request.userPreference().selectedCategories()
                + ", priorities=" + request.userPreference().shoppingPriorities()
                + ", style=" + request.userPreference().shoppingStyle().label()
                + ", favourite brands=" + request.userPreference().favoriteBrands();
        return "Extract shopping intent from the user's raw query. Return only the requested JSON object. "
                + "Do not invent requirements. The current query always overrides saved preferences. "
                + "Use null for unknown scalar values and empty arrays when there is no evidence. "
                + "Normalize category to one of Fashion, Footwear, Electronics, Beauty, Accessories, Home, Sports. "
                + "Convert phrases like 'my skin is dry' or 'for oily skin' into a concise useCases value. "
                + "Convert 'good brands', 'reputable brands', or 'known brands' into qualityPreference='reputable-brands' "
                + "and do not name brands unless the user names them. Convert 'best' or 'recommended' into a ranking priority. "
                + "Interpret k as thousand in budgets, such as 1k = 1000.\n"
                + preferences + "\nUser query: " + request.rawQuery();
    }

    private static Schema optionalString() {
        return Schema.builder().type(Type.Known.STRING).nullable(true).build();
    }

    private static Schema optionalNumber() {
        return Schema.builder().type(Type.Known.NUMBER).nullable(true).build();
    }
}
