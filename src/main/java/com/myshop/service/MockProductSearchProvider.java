package com.myshop.service;

import com.myshop.model.Product;
import com.myshop.model.SearchFilters;
import com.myshop.model.SearchRequest;
import com.myshop.model.SearchSortMode;
import com.myshop.model.ShoppingIntent;
import com.myshop.model.UserPreference;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class MockProductSearchProvider implements ProductSearchProvider {

    @Override
    public List<Product> search(SearchRequest request) {
        SearchFilters filters = request.filters();
        String query = request.rawQuery().toLowerCase(Locale.ROOT);
        String category = effectiveCategory(request).toLowerCase(Locale.ROOT);
        String brand = filters.brand() == null ? "" : filters.brand().toLowerCase(Locale.ROOT);

        List<Product> matches = MockCatalogService.searchableProducts().stream()
                .filter(product -> category.isBlank()
                        || product.category() != null && product.category().toLowerCase(Locale.ROOT).equals(category))
                .filter(product -> query.isBlank() || matchesQuery(product, query))
                .filter(product -> brand.isBlank()
                        || product.brand().toLowerCase(Locale.ROOT).contains(brand))
                .filter(product -> price(product) >= lowerBound(filters))
                .filter(product -> filters.maximumPrice() == null || price(product) <= filters.maximumPrice())
                .filter(product -> product.rating() >= filters.minimumRating())
                .sorted(comparator(request, query, category))
                .toList();
        return matches;
    }

    private boolean matchesQuery(Product product, String query) {
        return contains(product.name(), query)
                || contains(product.brand(), query)
                || contains(product.category(), query)
                || contains(product.description(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String effectiveCategory(SearchRequest request) {
        return request.filters().category() == null
                ? request.category() == null ? "" : request.category()
                : request.filters().category();
    }

    private double lowerBound(SearchFilters filters) {
        return filters.minimumPrice() == null ? 0 : filters.minimumPrice();
    }

    private double price(Product product) {
        return Double.parseDouble(product.price().replaceAll("[^0-9.]", ""));
    }

    private Comparator<Product> comparator(SearchRequest request, String query, String category) {
        Comparator<Product> stable = Comparator.comparing(Product::id);
        return switch (request.sortMode()) {
            case PRICE_LOW_TO_HIGH -> Comparator.comparingDouble(this::price).thenComparing(stable);
            case PRICE_HIGH_TO_LOW -> Comparator.comparingDouble(this::price).reversed().thenComparing(stable);
            case RATING -> Comparator.comparingDouble(Product::rating).reversed().thenComparing(stable);
            case RECOMMENDED -> Comparator.comparingDouble(
                    (Product product) -> recommendationScore(product, request, query, category)
            ).reversed().thenComparing(stable);
        };
    }

    private double recommendationScore(Product product, SearchRequest request, String query, String category) {
        double score = product.rating();
        if (!query.isBlank() && matchesQuery(product, query)) {
            score += 100;
        }
        if (!category.isBlank() && product.category() != null
                && product.category().equalsIgnoreCase(category)) {
            score += 75;
        }
        UserPreference preference = request.userPreference();
        ShoppingIntent intent = request.shoppingIntent();
        if (preference != null && (intent == null || intent.category() == null)) {
            if (preference.selectedCategories().stream()
                    .anyMatch(value -> value.equalsIgnoreCase(product.category()))) {
                score += 15;
            }
        }
        Set<String> favouriteBrands = intent != null && !intent.preferredBrands().isEmpty()
                ? Set.copyOf(intent.preferredBrands())
                : preference == null ? Set.of() : Set.of(preference.favoriteBrands().split(","));
        if (favouriteBrands.stream().anyMatch(value -> !value.isBlank()
                && product.brand().equalsIgnoreCase(value.trim()))) {
            score += 20;
        }
        UserPreference.ShoppingStyle style = intentStyle(intent)
                .orElse(preference == null ? UserPreference.ShoppingStyle.NO_PREFERENCE : preference.shoppingStyle());
        if (style == UserPreference.ShoppingStyle.PREMIUM && price(product) >= 150) {
            score += 4;
        }
        if (style == UserPreference.ShoppingStyle.BUDGET_CONSCIOUS && price(product) < 100) {
            score += 4;
        }
        return score;
    }

    private java.util.Optional<UserPreference.ShoppingStyle> intentStyle(ShoppingIntent intent) {
        if (intent == null || intent.shoppingStyle() == null) {
            return java.util.Optional.empty();
        }
        return switch (intent.shoppingStyle().toLowerCase(Locale.ROOT)) {
            case "premium" -> java.util.Optional.of(UserPreference.ShoppingStyle.PREMIUM);
            case "budget-conscious", "budget conscious" ->
                    java.util.Optional.of(UserPreference.ShoppingStyle.BUDGET_CONSCIOUS);
            case "best-value", "best value" -> java.util.Optional.of(UserPreference.ShoppingStyle.BEST_VALUE);
            default -> java.util.Optional.empty();
        };
    }
}
