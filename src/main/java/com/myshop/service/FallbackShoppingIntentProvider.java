package com.myshop.service;

import com.myshop.model.SearchRequest;
import com.myshop.model.ShoppingIntent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small deterministic parser used when a remote intent provider is unavailable. */
public final class FallbackShoppingIntentProvider implements ShoppingIntentProvider {

    private static final Pattern MAX_BUDGET = Pattern.compile(
            "(?i)(?:under|below|less than|up to|upto|within|max(?:imum)?)\\s*"
                    + "(?:₹|rs\\.?|inr|\\$)?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*(k|thousand)?"
    );
    private static final Pattern MIN_BUDGET = Pattern.compile(
            "(?i)(?:over|above|more than|at least|min(?:imum)?)\\s*"
                    + "(?:₹|rs\\.?|inr|\\$)?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*(k|thousand)?"
    );
    private static final List<String> COLORS = List.of(
            "black", "white", "blue", "green", "red", "brown", "beige", "grey", "gray", "pink"
    );
    private static final List<String> BRANDS = List.of(
            "Veja", "Marshall", "Uniqlo", "Seiko", "Aesop", "On Running", "Nike", "Adidas"
    );

    @Override
    public ShoppingIntent understand(SearchRequest request) {
        String query = request.rawQuery();
        String normalized = query.toLowerCase(Locale.ROOT);
        return new ShoppingIntent(
                query,
                category(normalized),
                productType(normalized),
                budget(MIN_BUDGET, normalized),
                budget(MAX_BUDGET, normalized),
                matchingLabels(BRANDS, normalized),
                matchingLabels(COLORS, normalized),
                useCases(normalized),
                matchingTerms(normalized, List.of(
                        "comfort", "comfortable", "minimal", "minimalist", "quality", "durable",
                        "style", "rating", "ratings", "premium", "luxury", "value", "best", "recommended"
                )),
                keywords(normalized),
                shoppingStyle(normalized),
                qualityPreference(normalized)
        );
    }

    private String category(String query) {
        if (containsAny(query, "sneaker", "shoe", "footwear", "boots", "sandals")) return "Footwear";
        if (containsAny(query, "headphone", "earbuds", "laptop", "phone", "tablet", "camera")) return "Electronics";
        if (containsAny(query, "shirt", "dress", "jacket", "jeans", "clothing", "fashion")) return "Fashion";
        if (containsAny(query, "skincare", "skin care", "makeup", "beauty", "serum", "moisturizer",
                "moisturiser", "cleanser", "sunscreen", "fragrance", "perfume")) return "Beauty";
        if (containsAny(query, "watch", "wallet", "bag", "jewelry", "accessory")) return "Accessories";
        if (containsAny(query, "furniture", "home", "kitchen", "glass", "decor")) return "Home";
        if (containsAny(query, "sports", "running", "gym", "fitness", "yoga")) return "Sports";
        return null;
    }

    private String productType(String query) {
        for (String type : List.of("sneakers", "headphones", "earbuds", "moisturizer", "moisturiser",
                "cleanser", "sunscreen", "serum", "shirt", "jacket", "watch", "bag")) {
            if (query.contains(type)) return "moisturiser".equals(type) ? "moisturizer" : type;
        }
        return null;
    }

    private Double budget(Pattern pattern, String query) {
        Matcher matcher = pattern.matcher(query);
        if (!matcher.find()) return null;
        try {
            double value = Double.parseDouble(matcher.group(1).replace(",", ""));
            String suffix = matcher.group(2);
            return suffix == null || suffix.isBlank() ? value : value * 1000;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<String> matchingLabels(List<String> labels, String query) {
        return labels.stream()
                .filter(label -> query.contains(label.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private List<String> matchingTerms(String query, List<String> terms) {
        return terms.stream().filter(query::contains).toList();
    }

    private List<String> keywords(String query) {
        Set<String> values = new LinkedHashSet<>();
        String type = productType(query);
        if (type != null) values.add(type);
        values.addAll(useCases(query));
        values.addAll(matchingLabels(COLORS, query));
        values.addAll(matchingTerms(query, List.of("comfortable", "minimal", "college", "daily wear", "everyday", "premium")));
        if (values.isEmpty() && !query.isBlank()) {
            values.addAll(conciseTokens(query));
        }
        return new ArrayList<>(values);
    }

    private List<String> useCases(String query) {
        List<String> values = new ArrayList<>();
        if (containsAny(query, "dry skin", "skin is dry", "skin feels dry")) values.add("dry skin");
        if (containsAny(query, "oily skin", "skin is oily", "skin feels oily")) values.add("oily skin");
        values.addAll(matchingTerms(query, List.of(
                "college", "daily wear", "everyday", "work", "office", "travel", "running", "gym",
                "gaming", "walking", "battery"
        )));
        return values.stream().distinct().toList();
    }

    private List<String> conciseTokens(String query) {
        return List.of(query.replaceAll("[^a-z0-9 ]", " ").split("\\s+"));
    }

    private String shoppingStyle(String query) {
        if (containsAny(query, "premium", "luxury", "price doesn't matter", "price does not matter")) return "premium";
        if (containsAny(query, "budget", "affordable", "cheap", "low cost")) return "budget-conscious";
        if (containsAny(query, "best value", "good value", "value for money")) return "best-value";
        return null;
    }

    private String qualityPreference(String query) {
        return containsAny(query, "good brand", "good brands", "reputable brand", "reputable brands",
                "known brand", "known brands", "trusted brand", "trusted brands")
                ? "reputable-brands" : null;
    }

    private boolean containsAny(String query, String... values) {
        for (String value : values) if (query.contains(value)) return true;
        return false;
    }
}
