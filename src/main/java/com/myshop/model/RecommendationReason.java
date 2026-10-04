package com.myshop.model;

import java.util.Objects;

/** A concise explanation backed by a product or request signal. */
public record RecommendationReason(Type type, String text) {

    public RecommendationReason {
        Objects.requireNonNull(type, "type");
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        text = text.trim();
    }

    public enum Type {
        PRODUCT_MATCH,
        CATEGORY_MATCH,
        BUDGET,
        COLOR_MATCH,
        BRAND_MATCH,
        USE_CASE,
        PRIORITY,
        PREFERENCE_CATEGORY,
        FAVORITE_BRAND,
        RATING
    }
}
