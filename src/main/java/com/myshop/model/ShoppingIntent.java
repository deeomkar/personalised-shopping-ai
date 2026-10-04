package com.myshop.model;

import java.util.List;
import java.util.Objects;

/** Structured, provider-neutral shopping intent extracted from a search request. */
public record ShoppingIntent(
        String rawQuery,
        String category,
        String productType,
        Double minBudget,
        Double maxBudget,
        List<String> preferredBrands,
        List<String> colors,
        List<String> useCases,
        List<String> priorities,
        List<String> keywords,
        String shoppingStyle
) {
    public ShoppingIntent {
        rawQuery = normalize(rawQuery);
        category = normalizeNullable(category);
        productType = normalizeNullable(productType);
        minBudget = validBudget(minBudget, "minBudget");
        maxBudget = validBudget(maxBudget, "maxBudget");
        if (minBudget != null && maxBudget != null && minBudget > maxBudget) {
            throw new IllegalArgumentException("minBudget cannot exceed maxBudget");
        }
        preferredBrands = immutableList(preferredBrands);
        colors = immutableList(colors);
        useCases = immutableList(useCases);
        priorities = immutableList(priorities);
        keywords = immutableList(keywords);
        shoppingStyle = normalizeNullable(shoppingStyle);
    }

    public ShoppingIntent(String rawQuery, String category, String productType,
                          Double minBudget, Double maxBudget, List<String> preferredBrands,
                          List<String> colors, List<String> useCases, List<String> priorities,
                          List<String> keywords) {
        this(rawQuery, category, productType, minBudget, maxBudget, preferredBrands,
                colors, useCases, priorities, keywords, null);
    }

    public static ShoppingIntent empty(String rawQuery) {
        return new ShoppingIntent(rawQuery, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), null);
    }

    private static Double validBudget(Double value, String name) {
        if (value == null) {
            return null;
        }
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
        return value;
    }

    private static List<String> immutableList(List<String> values) {
        Objects.requireNonNullElse(values, List.<String>of()).forEach(value -> {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("intent list values must not be blank");
            }
        });
        return List.copyOf(values == null ? List.of() : values);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }
}
