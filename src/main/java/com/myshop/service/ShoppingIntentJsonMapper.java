package com.myshop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.model.SearchRequest;
import com.myshop.model.ShoppingIntent;

import java.util.List;

/** Maps Gemini's schema-constrained JSON into the provider-neutral intent model. */
public final class ShoppingIntentJsonMapper {
    private final ObjectMapper objectMapper;

    public ShoppingIntentJsonMapper() {
        this(new ObjectMapper());
    }

    ShoppingIntentJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ShoppingIntent map(String json, SearchRequest request) {
        try {
            Payload payload = objectMapper.readValue(json, Payload.class);
            return new ShoppingIntent(
                    request.rawQuery(),
                    payload.category,
                    payload.productType,
                    payload.minBudget,
                    payload.maxBudget,
                    safe(payload.preferredBrands),
                    safe(payload.colors),
                    safe(payload.useCases),
                    safe(payload.priorities),
                    safe(payload.keywords),
                    payload.shoppingStyle
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException("Gemini returned malformed shopping intent JSON", exception);
        }
    }

    private List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static final class Payload {
        public String category;
        public String productType;
        public Double minBudget;
        public Double maxBudget;
        public List<String> preferredBrands;
        public List<String> colors;
        public List<String> useCases;
        public List<String> priorities;
        public List<String> keywords;
        public String shoppingStyle;
    }
}
