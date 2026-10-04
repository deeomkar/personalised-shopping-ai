package com.myshop.model;

import java.util.Optional;

public record SearchRequest(
        String rawQuery,
        String category,
        long userId,
        UserPreference userPreference,
        SearchFilters filters,
        SearchSortMode sortMode,
        ShoppingIntent shoppingIntent
) {
    public SearchRequest(String rawQuery, String category, long userId,
                         UserPreference userPreference, SearchFilters filters,
                         SearchSortMode sortMode) {
        this(rawQuery, category, userId, userPreference, filters, sortMode, null);
    }

    public SearchRequest {
        rawQuery = normalize(rawQuery);
        category = normalize(category);
        filters = filters == null ? SearchFilters.none() : filters;
        sortMode = sortMode == null ? SearchSortMode.RECOMMENDED : sortMode;
    }

    public Optional<UserPreference> preference() {
        return Optional.ofNullable(userPreference);
    }

    public boolean hasSearchIntent() {
        return !rawQuery.isBlank()
                || category != null
                || filters.category() != null
                || filters.brand() != null
                || filters.minimumPrice() != null
                || filters.maximumPrice() != null
                || filters.minimumRating() > 0;
    }

    public SearchRequest withRawQuery(String query) {
        return new SearchRequest(query, category, userId, userPreference, filters, sortMode, shoppingIntent);
    }

    public SearchRequest withCategory(String nextCategory) {
        return new SearchRequest(rawQuery, nextCategory, userId, userPreference, filters, sortMode, shoppingIntent);
    }

    public SearchRequest withFilters(SearchFilters nextFilters) {
        return new SearchRequest(rawQuery, category, userId, userPreference, nextFilters, sortMode, shoppingIntent);
    }

    public SearchRequest withSortMode(SearchSortMode nextSortMode) {
        return new SearchRequest(rawQuery, category, userId, userPreference, filters, nextSortMode, shoppingIntent);
    }

    public SearchRequest withShoppingIntent(ShoppingIntent nextIntent) {
        return new SearchRequest(rawQuery, category, userId, userPreference, filters, sortMode, nextIntent);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

}
