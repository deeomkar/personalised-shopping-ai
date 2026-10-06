package com.myshop.service;

import com.myshop.model.Product;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Deterministic product-type matching used to keep unrelated provider results out of recommendations. */
final class ProductTypeRelevance {

    private static final Pattern WORDS = Pattern.compile("[^\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Set<String> ACCESSORY_TERMS = Set.of(
            "accessory", "accessories", "brush", "cable", "case", "cleaner", "cover", "lace",
            "polish", "replacement", "sponge", "stand", "strap", "winder"
    );

    private ProductTypeRelevance() {
    }

    static List<Product> filter(String productType, List<Product> products) {
        if (productType == null || productType.isBlank()) {
            return List.copyOf(products == null ? List.of() : products);
        }
        return (products == null ? List.<Product>of() : products).stream()
                .filter(product -> product != null && matches(product, productType))
                .toList();
    }

    static boolean matches(Product product, String productType) {
        if (product == null || productType == null || productType.isBlank()) {
            return true;
        }
        Set<String> aliases = aliases(productType);
        String primaryText = searchableText(product.brand(), product.name(), product.category());
        String description = normalize(product.description());
        if (containsAlias(primaryText, aliases)) {
            return !isAccessoryResult(product.name(), productType);
        }
        return containsAlias(description, aliases);
    }

    private static Set<String> aliases(String productType) {
        String normalized = normalize(productType);
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (normalized.contains("foundation")) {
            values.addAll(List.of("foundation", "face foundation", "makeup foundation"));
        } else if (normalized.contains("sneaker") || normalized.contains("trainer")) {
            values.addAll(List.of("sneaker", "sneakers", "shoe", "shoes", "trainer", "trainers"));
        } else if (normalized.contains("headphone") || normalized.contains("headset")
                || normalized.contains("earbud")) {
            values.addAll(List.of("headphone", "headphones", "headset", "earbud", "earbuds"));
        } else if (normalized.contains("moisturizer") || normalized.contains("moisturiser")) {
            values.addAll(List.of("moisturizer", "moisturisers", "moisturiser", "moisturizers",
                    "moisturising cream", "moisturizing cream", "hydrating cream"));
        } else if (normalized.contains("watch")) {
            values.addAll(List.of("watch", "watches", "timepiece"));
        }
        values.add(normalized);
        if (normalized.endsWith("s") && normalized.length() > 3) {
            values.add(normalized.substring(0, normalized.length() - 1));
        }
        return values;
    }

    private static boolean containsAlias(String text, Set<String> aliases) {
        List<String> textTokens = tokens(text);
        for (String alias : aliases) {
            List<String> aliasTokens = tokens(alias);
            if (!aliasTokens.isEmpty() && containsSequence(textTokens, aliasTokens)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSequence(List<String> textTokens, List<String> aliasTokens) {
        if (aliasTokens.size() > textTokens.size()) {
            return false;
        }
        for (int start = 0; start <= textTokens.size() - aliasTokens.size(); start++) {
            boolean matches = true;
            for (int offset = 0; offset < aliasTokens.size(); offset++) {
                if (!textTokens.get(start + offset).equals(aliasTokens.get(offset))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAccessoryResult(String name, String productType) {
        String normalizedName = normalize(name);
        if (normalizedName.isBlank()) {
            return false;
        }
        boolean accessory = tokenSet(normalizedName).stream().anyMatch(ACCESSORY_TERMS::contains);
        if (!accessory) {
            return false;
        }
        String type = normalize(productType);
        return (type.contains("foundation") && tokenSet(normalizedName).stream()
                .anyMatch(Set.of("brush", "sponge", "accessory")::contains))
                || ((type.contains("headphone") || type.contains("headset") || type.contains("earbud"))
                && tokenSet(normalizedName).stream().anyMatch(Set.of("case", "cover", "cable", "stand")::contains))
                || ((type.contains("sneaker") || type.contains("trainer") || type.contains("shoe"))
                && tokenSet(normalizedName).stream().anyMatch(Set.of("cleaner", "lace", "polish")::contains))
                || (type.contains("watch") && tokenSet(normalizedName).stream()
                .anyMatch(Set.of("strap", "band", "case", "winder")::contains));
    }

    private static String searchableText(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append(' ');
                }
                builder.append(value);
            }
        }
        return normalize(builder.toString());
    }

    private static List<String> tokens(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? List.of() : List.of(WORDS.split(normalized));
    }

    private static Set<String> tokenSet(String value) {
        return Set.copyOf(tokens(value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
