package com.myshop.service;

import com.myshop.model.Product;
import com.myshop.model.BehaviorProfile;
import com.myshop.model.Recommendation;
import com.myshop.model.RecommendationReason;
import com.myshop.model.SearchRequest;
import com.myshop.model.SearchSortMode;
import com.myshop.model.ShoppingIntent;
import com.myshop.model.UserPreference;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Deterministic local ranking for products already returned by a provider. */
public final class RecommendationService {

    // Current query signals intentionally dominate persisted preference signals.
    static final double PRODUCT_TYPE_WEIGHT = 28;
    static final double QUERY_MATCH_WEIGHT = 14;
    static final double CATEGORY_WEIGHT = 16;
    static final double BUDGET_WEIGHT = 18;
    static final double COLOR_WEIGHT = 10;
    static final double BRAND_WEIGHT = 8;
    static final double USE_CASE_WEIGHT = 6;
    static final double PRIORITY_WEIGHT = 6;
    static final double PREFERENCE_CATEGORY_WEIGHT = 4;
    static final double PREFERENCE_PRIORITY_WEIGHT = 2;
    static final double FAVORITE_BRAND_WEIGHT = 4;
    static final double RATING_WEIGHT = 3;
    static final double MAX_SCORE = 120;

    private static final Pattern WORDS = Pattern.compile("[^\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Set<String> QUERY_STOP_WORDS = Set.of(
            "a", "an", "and", "for", "from", "i", "in", "me", "my", "need", "of", "on", "the",
            "to", "under", "with", "within", "show", "find", "looking", "want", "please", "price",
            "budget", "below", "less", "than", "upto", "up", "rs", "inr"
    );

    public List<Recommendation> rank(SearchRequest request, List<Product> products) {
        return rank(request, products, BehaviorProfile.empty());
    }

    public List<Recommendation> rank(SearchRequest request, List<Product> products, BehaviorProfile behaviorProfile) {
        Objects.requireNonNull(request, "request");
        List<Product> safeProducts = List.copyOf(products == null ? List.of() : products);
        List<Recommendation> recommendations = safeProducts.stream()
                .map(product -> score(request, product, safeProducts,
                        behaviorProfile == null ? BehaviorProfile.empty() : behaviorProfile))
                .toList();

        Comparator<Recommendation> comparator = switch (request.sortMode()) {
            case PRICE_LOW_TO_HIGH -> Comparator.comparingDouble(this::priceForAscendingSort)
                    .thenComparing(recommendation -> recommendation.product().id());
            case PRICE_HIGH_TO_LOW -> Comparator.comparing(
                            (Recommendation recommendation) -> parsePrice(recommendation.product().price()),
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(recommendation -> recommendation.product().id());
            case RATING -> Comparator.comparingDouble((Recommendation recommendation) ->
                            safeRating(recommendation.product())).reversed()
                    .thenComparing(recommendation -> recommendation.product().id());
            case RECOMMENDED -> Comparator.comparingDouble(Recommendation::score).reversed()
                    .thenComparing(Comparator.comparingDouble(
                            (Recommendation recommendation) -> safeRating(recommendation.product())
                    ).reversed())
                    .thenComparing(recommendation -> recommendation.product().id());
        };
        return recommendations.stream().sorted(comparator).toList();
    }

    private Recommendation score(SearchRequest request, Product product, List<Product> products,
                                 BehaviorProfile behaviorProfile) {
        ShoppingIntent intent = request.shoppingIntent();
        UserPreference preference = request.userPreference();
        String searchable = searchableText(product);
        List<RecommendationReason> reasons = new ArrayList<>();
        double points = 0;

        if (intent != null && intent.productType() != null && matches(searchable, intent.productType())) {
            points += PRODUCT_TYPE_WEIGHT;
            reasons.add(new RecommendationReason(RecommendationReason.Type.PRODUCT_MATCH,
                    "Matches " + intent.productType()));
        }

        List<String> queryTerms = queryTerms(request, intent);
        long matchedTerms = queryTerms.stream().filter(term -> matches(searchable, term)).count();
        if (!queryTerms.isEmpty() && matchedTerms > 0) {
            points += QUERY_MATCH_WEIGHT * matchedTerms / queryTerms.size();
        }

        String requestedCategory = effectiveCategory(request, intent);
        if (requestedCategory != null && product.category() != null
                && product.category().equalsIgnoreCase(requestedCategory)) {
            points += CATEGORY_WEIGHT;
            reasons.add(new RecommendationReason(RecommendationReason.Type.CATEGORY_MATCH,
                    "Matches your " + requestedCategory.toLowerCase(Locale.ROOT) + " search"));
        }

        Double maxBudget = effectiveMaxBudget(request, intent);
        Double minBudget = effectiveMinBudget(request, intent);
        Double productPrice = parsePrice(product.price());
        if (productPrice != null && maxBudget != null) {
            if (productPrice <= maxBudget) {
                points += BUDGET_WEIGHT;
                reasons.add(new RecommendationReason(RecommendationReason.Type.BUDGET,
                        "Within your " + formatBudget(maxBudget, request.rawQuery()) + " budget"));
            } else {
                points -= BUDGET_WEIGHT * 2;
            }
        }
        if (productPrice != null && minBudget != null) {
            if (productPrice >= minBudget) {
                points += BUDGET_WEIGHT / 2;
            } else {
                points -= BUDGET_WEIGHT;
            }
        }

        if (intent != null && !intent.colors().isEmpty()) {
            List<String> matchedColors = intent.colors().stream()
                    .filter(color -> matches(searchable, color)).toList();
            if (!matchedColors.isEmpty()) {
                points += COLOR_WEIGHT;
                reasons.add(new RecommendationReason(RecommendationReason.Type.COLOR_MATCH,
                        "Matches your " + String.join(" and ", matchedColors) + " color preference"));
            }
        }

        if (intent != null && !intent.preferredBrands().isEmpty()) {
            String brand = nullToEmpty(product.brand());
            if (intent.preferredBrands().stream().anyMatch(value -> brand.equalsIgnoreCase(value))) {
                points += BRAND_WEIGHT;
                reasons.add(new RecommendationReason(RecommendationReason.Type.BRAND_MATCH,
                        "Matches your preferred brand"));
            }
        }

        if (intent != null && !intent.useCases().isEmpty()
                && intent.useCases().stream().anyMatch(value -> matches(searchable, value))) {
            points += USE_CASE_WEIGHT;
            reasons.add(new RecommendationReason(RecommendationReason.Type.USE_CASE,
                    "Matches your " + intent.useCases().getFirst() + " search"));
        }

        if (intent != null && !intent.priorities().isEmpty()
                && intent.priorities().stream().anyMatch(value -> matches(searchable, value))) {
            points += PRIORITY_WEIGHT;
            reasons.add(new RecommendationReason(RecommendationReason.Type.PRIORITY,
                    "Matches your " + intent.priorities().getFirst() + " priority"));
        }

        if (preference != null) {
            if ((intent == null || intent.category() == null)
                    && preference.selectedCategories().stream().anyMatch(value ->
                    value.equalsIgnoreCase(nullToEmpty(product.category())))) {
                points += PREFERENCE_CATEGORY_WEIGHT;
                reasons.add(new RecommendationReason(RecommendationReason.Type.PREFERENCE_CATEGORY,
                        "In one of your preferred categories"));
            }
            if ((intent == null || intent.priorities().isEmpty())
                    && preference.shoppingPriorities().stream().anyMatch(value -> matches(searchable, value))) {
                points += PREFERENCE_PRIORITY_WEIGHT;
            }
            if ((intent == null || intent.preferredBrands().isEmpty())
                    && favoriteBrands(preference.favoriteBrands()).stream()
                    .anyMatch(value -> value.equalsIgnoreCase(nullToEmpty(product.brand())))) {
                points += FAVORITE_BRAND_WEIGHT;
                reasons.add(new RecommendationReason(RecommendationReason.Type.FAVORITE_BRAND,
                        "Matches one of your preferred brands"));
            }
            if (shouldUseSavedStyle(intent, preference, productPrice, products)) {
                points += 2;
            }
        }

        if (behaviorProfile.categories().stream().anyMatch(value ->
                value.equalsIgnoreCase(nullToEmpty(product.category())))) {
            points += 2;
        }
        if (behaviorProfile.brands().stream().anyMatch(value ->
                value.equalsIgnoreCase(nullToEmpty(product.brand())))) {
            points += 1;
        }

        double rating = safeRating(product);
        if (rating > 0) {
            points += (rating / 5) * RATING_WEIGHT;
            if (rating >= 4.5) {
                reasons.add(new RecommendationReason(RecommendationReason.Type.RATING,
                        "Highly rated at " + String.format(Locale.ROOT, "%.1f", rating)));
            }
        }

        double normalizedScore = Math.max(0, Math.min(100, points / MAX_SCORE * 100));
        return new Recommendation(product, normalizedScore, reasons);
    }

    private boolean shouldUseSavedStyle(ShoppingIntent intent, UserPreference preference,
                                        Double productPrice, List<Product> products) {
        // Explicit current budget/style intent wins over onboarding style.
        if (preference == null || preference.shoppingStyle() == UserPreference.ShoppingStyle.NO_PREFERENCE
                || productPrice == null
                || intent != null && (intent.shoppingStyle() != null
                || intent.minBudget() != null || intent.maxBudget() != null)) {
            return false;
        }
        List<Double> prices = products.stream().map(product -> parsePrice(product.price()))
                .filter(Objects::nonNull).sorted().toList();
        if (prices.isEmpty()) {
            return false;
        }
        double midpoint = prices.get(prices.size() / 2);
        return switch (preference.shoppingStyle()) {
            case BUDGET_CONSCIOUS, BEST_VALUE -> productPrice <= midpoint;
            case PREMIUM -> productPrice >= midpoint;
            case NO_PREFERENCE -> false;
        };
    }

    private String effectiveCategory(SearchRequest request, ShoppingIntent intent) {
        if (request.filters().category() != null) {
            return request.filters().category();
        }
        if (request.category() != null) {
            return request.category();
        }
        return intent == null ? null : intent.category();
    }

    private Double effectiveMaxBudget(SearchRequest request, ShoppingIntent intent) {
        return request.filters().maximumPrice() != null
                ? request.filters().maximumPrice() : intent == null ? null : intent.maxBudget();
    }

    private Double effectiveMinBudget(SearchRequest request, ShoppingIntent intent) {
        return request.filters().minimumPrice() != null
                ? request.filters().minimumPrice() : intent == null ? null : intent.minBudget();
    }

    private List<String> queryTerms(SearchRequest request, ShoppingIntent intent) {
        Set<String> terms = new HashSet<>();
        addMeaningfulTerms(terms, request.rawQuery());
        if (intent != null) {
            intent.keywords().forEach(value -> addMeaningfulTerms(terms, value));
        }
        return terms.stream().sorted().toList();
    }

    private void addMeaningfulTerms(Set<String> terms, String value) {
        if (value == null) {
            return;
        }
        for (String token : WORDS.split(value.toLowerCase(Locale.ROOT))) {
            if (token.length() > 2 && !QUERY_STOP_WORDS.contains(token) && !token.matches("\\d+")) {
                terms.add(token);
            }
        }
    }

    private boolean matches(String searchable, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT).trim();
        if (searchable.contains(normalized)) {
            return true;
        }
        if (normalized.endsWith("s") && normalized.length() > 3) {
            return searchable.contains(normalized.substring(0, normalized.length() - 1));
        }
        return normalized.endsWith("y") && normalized.length() > 3
                && searchable.contains(normalized.substring(0, normalized.length() - 1) + "ies");
    }

    private String searchableText(Product product) {
        return String.join(" ", nullToEmpty(product.brand()), nullToEmpty(product.name()),
                nullToEmpty(product.category()), nullToEmpty(product.description()), nullToEmpty(product.store()))
                .toLowerCase(Locale.ROOT);
    }

    private Set<String> favoriteBrands(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Set.of(value.split(",")).stream().map(String::trim).filter(item -> !item.isBlank()).collect(java.util.stream.Collectors.toSet());
    }

    private double priceForAscendingSort(Recommendation recommendation) {
        Double price = parsePrice(recommendation.product().price());
        return price == null ? Double.POSITIVE_INFINITY : price;
    }

    private Double parsePrice(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String numeric = value.replace(",", "").replaceAll("[^0-9.]", "");
        if (numeric.isBlank()) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(numeric);
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private double safeRating(Product product) {
        return Double.isFinite(product.rating()) ? Math.max(0, product.rating()) : 0;
    }

    private String formatBudget(double value, String query) {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.ROOT);
        format.setGroupingUsed(true);
        String symbol = query != null && (query.contains("₹") || query.toLowerCase(Locale.ROOT).contains("inr"))
                ? "₹" : query != null && query.contains("$") ? "$" : "";
        return symbol + format.format(value);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
