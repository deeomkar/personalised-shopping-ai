package com.myshop.service;

import com.myshop.model.Product;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Deterministic, provider-neutral product-type matching used for recommendation safety. */
final class ProductTypeRelevance {

    private static final Pattern WORDS = Pattern.compile("[^\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final int MANY_PROVIDER_RESULTS = 10;
    private static final int WEAK_STRICT_MATCHES = 2;

    /* Data-driven variants only. Unknown product types use the same token matcher automatically. */
    private static final Map<String, Set<String>> SYNONYMS = Map.of(
            "sunscreen", Set.of("sunscreen", "sun screen", "sunblock", "sun block", "spf"),
            "moisturizer", Set.of("moisturizer", "moisturiser", "hydrating cream"),
            "foundation", Set.of("foundation", "face foundation", "makeup foundation"),
            "sneaker", Set.of("sneaker", "shoe", "trainer"),
            "headphone", Set.of("headphone", "headset", "earbud"),
            "watch", Set.of("watch", "timepiece")
    );

    private static final Map<String, Set<String>> ACCESSORY_BLOCKERS = Map.of(
            "foundation", Set.of("brush", "sponge", "accessory"),
            "headphone", Set.of("case", "cover", "cable", "stand"),
            "sneaker", Set.of("cleaner", "lace", "polish"),
            "watch", Set.of("strap", "band", "case", "winder")
    );

    private static final Set<String> TYPE_BOUNDARIES = Set.of(
            "for", "with", "under", "below", "over", "above", "within", "between", "from", "in"
    );

    private ProductTypeRelevance() {
    }

    static List<Product> filter(String productType, List<Product> products) {
        List<Product> safeProducts = products == null ? List.of() : products.stream()
                .filter(product -> product != null)
                .toList();
        if (productType == null || productType.isBlank()) {
            return safeProducts;
        }

        List<Product> strictMatches = safeProducts.stream()
                .filter(product -> matches(product, productType))
                .toList();
        if (safeProducts.size() >= MANY_PROVIDER_RESULTS && strictMatches.size() <= WEAK_STRICT_MATCHES) {
            List<Product> relaxedMatches = safeProducts.stream()
                    .filter(product -> relaxedMatches(product, productType))
                    .toList();
            if (!relaxedMatches.isEmpty()) {
                return relaxedMatches;
            }
            /* A provider result set is useful evidence even when vocabulary differs from our aliases. */
            if (strictMatches.isEmpty() && !isKnownType(productType)) {
                return safeProducts;
            }
        }
        /* Unknown product types must not fail closed on small provider result sets either. */
        return strictMatches.isEmpty() && !isKnownType(productType) ? safeProducts : strictMatches;
    }

    static boolean matches(Product product, String productType) {
        if (product == null || productType == null || productType.isBlank()) {
            return true;
        }
        String core = coreProductType(productType);
        Set<String> aliases = aliases(core);
        if (!containsAlias(searchableText(product), aliases)) {
            return false;
        }
        return !isAccessoryResult(product.name(), core);
    }

    static String coreProductType(String productType) {
        List<String> candidate = tokensBeforeBoundary(productType);
        return String.join(" ", candidate);
    }

    private static boolean relaxedMatches(Product product, String productType) {
        String core = coreProductType(productType);
        List<String> coreTokens = tokens(core);
        if (coreTokens.isEmpty()) {
            return true;
        }
        Set<String> aliases = aliases(core);
        String text = searchableText(product);
        boolean textualMatch = containsAlias(text, aliases)
                || coreTokens.stream().anyMatch(token -> tokenSet(text).contains(token));
        return textualMatch && !isAccessoryResult(product.name(), core);
    }

    private static Set<String> aliases(String core) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        String canonical = canonicalAliasKey(core);
        if (canonical != null) {
            values.addAll(SYNONYMS.get(canonical));
        }
        if (!core.isBlank()) {
            values.add(core);
            List<String> coreTokens = tokens(core);
            if (!coreTokens.isEmpty()) {
                String head = coreTokens.getLast();
                values.add(head);
                values.add(singularize(head));
            }
        }
        return values;
    }

    private static String canonicalAliasKey(String core) {
        List<String> coreTokens = tokens(core);
        for (Map.Entry<String, Set<String>> entry : SYNONYMS.entrySet()) {
            if (entry.getValue().stream().anyMatch(alias -> containsSequence(coreTokens, tokens(alias)))) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static boolean isKnownType(String productType) {
        return canonicalAliasKey(coreProductType(productType)) != null;
    }

    private static boolean containsAlias(String text, Set<String> aliases) {
        List<String> textTokens = tokens(text);
        return aliases.stream().map(ProductTypeRelevance::tokens)
                .anyMatch(aliasTokens -> !aliasTokens.isEmpty() && containsSequence(textTokens, aliasTokens));
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
        String canonical = canonicalAliasKey(productType);
        if (canonical == null) {
            return false;
        }
        Set<String> blockers = ACCESSORY_BLOCKERS.get(canonical);
        if (blockers == null) {
            return false;
        }
        Set<String> nameTokens = tokenSet(name);
        return blockers.stream().map(ProductTypeRelevance::singularize)
                .anyMatch(nameTokens::contains);
    }

    private static String searchableText(Product product) {
        return normalize(String.join(" ", List.of(
                nullToEmpty(product.brand()), nullToEmpty(product.name()), nullToEmpty(product.category()),
                nullToEmpty(product.description())
        )));
    }

    private static List<String> tokensBeforeBoundary(String value) {
        List<String> result = new ArrayList<>();
        for (String token : tokens(value)) {
            if (TYPE_BOUNDARIES.contains(token)) {
                break;
            }
            result.add(token);
        }
        return result;
    }

    private static List<String> tokens(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(WORDS.split(normalized)).stream()
                .map(ProductTypeRelevance::singularize)
                .toList();
    }

    private static Set<String> tokenSet(String value) {
        return Set.copyOf(tokens(value));
    }

    private static String singularize(String value) {
        if (value == null || value.length() < 4) {
            return value;
        }
        if (value.endsWith("ies") && value.length() > 4) {
            return value.substring(0, value.length() - 3) + "y";
        }
        if (value.endsWith("s") && !value.endsWith("ss")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
