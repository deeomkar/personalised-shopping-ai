package com.myshop.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record UserPreference(
        long userId,
        Set<String> selectedCategories,
        Set<String> shoppingPriorities,
        ShoppingStyle shoppingStyle,
        String favoriteBrands,
        boolean onboardingCompleted,
        Instant updatedAt
) {
    public UserPreference {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        selectedCategories = immutableSet(selectedCategories, "selectedCategories");
        shoppingPriorities = immutableSet(shoppingPriorities, "shoppingPriorities");
        Objects.requireNonNull(shoppingStyle, "shoppingStyle");
        favoriteBrands = favoriteBrands == null ? "" : favoriteBrands.trim();
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    private static Set<String> immutableSet(Set<String> values, String name) {
        Objects.requireNonNull(values, name);
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(name + " cannot contain null values");
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    public enum ShoppingStyle {
        BUDGET_CONSCIOUS("Budget-conscious"),
        BEST_VALUE("Best value"),
        PREMIUM("Premium"),
        NO_PREFERENCE("No preference");

        private final String label;

        ShoppingStyle(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static ShoppingStyle fromLabel(String label) {
            for (ShoppingStyle style : values()) {
                if (style.label.equals(label)) {
                    return style;
                }
            }
            throw new IllegalArgumentException("Unknown shopping style: " + label);
        }
    }
}
