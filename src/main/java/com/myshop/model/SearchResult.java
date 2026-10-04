package com.myshop.model;

import java.util.List;

public record SearchResult(
        SearchRequest request,
        List<Product> products,
        int totalResultCount,
        SearchFilters appliedFilters,
        String message,
        SearchState state,
        List<Recommendation> recommendations
) {
    /** Backwards-compatible result constructor for callers without ranking metadata. */
    public SearchResult(SearchRequest request, List<Product> products, int totalResultCount,
                        SearchFilters appliedFilters, String message, SearchState state) {
        this(request, products, totalResultCount, appliedFilters, message, state, List.of());
    }

    public SearchResult {
        products = List.copyOf(products == null ? List.of() : products);
        appliedFilters = appliedFilters == null ? SearchFilters.none() : appliedFilters;
        message = message == null ? "" : message;
        state = state == null ? SearchState.SUCCESS : state;
        recommendations = List.copyOf(recommendations == null ? List.of() : recommendations);
    }

    public static SearchResult success(SearchRequest request, List<Product> products) {
        return new SearchResult(request, products, products.size(), request.filters(), "", SearchState.SUCCESS,
                List.of());
    }

    public static SearchResult successWithRecommendations(SearchRequest request,
                                                          List<Recommendation> recommendations) {
        List<Recommendation> safeRecommendations = List.copyOf(
                recommendations == null ? List.of() : recommendations
        );
        List<Product> products = safeRecommendations.stream().map(Recommendation::product).toList();
        return new SearchResult(request, products, products.size(), request.filters(), "", SearchState.SUCCESS,
                safeRecommendations);
    }

    public static SearchResult error(SearchRequest request, String message) {
        return new SearchResult(request, List.of(), 0, request.filters(), message, SearchState.ERROR, List.of());
    }

    public boolean isEmpty() {
        return state == SearchState.SUCCESS && products.isEmpty();
    }

    public enum SearchState {
        SUCCESS,
        ERROR
    }
}
