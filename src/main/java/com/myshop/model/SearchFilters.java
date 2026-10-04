package com.myshop.model;

public record SearchFilters(
        String category,
        String brand,
        Double minimumPrice,
        Double maximumPrice,
        double minimumRating
) {
    public SearchFilters {
        category = normalize(category);
        brand = normalize(brand);
        if (minimumPrice != null && (!Double.isFinite(minimumPrice) || minimumPrice < 0)) {
            throw new IllegalArgumentException("minimumPrice cannot be negative");
        }
        if (maximumPrice != null && (!Double.isFinite(maximumPrice) || maximumPrice < 0)) {
            throw new IllegalArgumentException("maximumPrice cannot be negative");
        }
        if (minimumPrice != null && maximumPrice != null && minimumPrice > maximumPrice) {
            throw new IllegalArgumentException("minimumPrice cannot exceed maximumPrice");
        }
        if (!Double.isFinite(minimumRating) || minimumRating < 0 || minimumRating > 5) {
            throw new IllegalArgumentException("minimumRating must be between 0 and 5");
        }
    }

    public static SearchFilters none() {
        return new SearchFilters(null, null, null, null, 0);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
