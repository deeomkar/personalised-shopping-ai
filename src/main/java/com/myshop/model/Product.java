package com.myshop.model;

import com.myshop.component.IconType;

import java.util.List;
import java.util.Locale;

public record Product(
        String id,
        String brand,
        String name,
        String category,
        String price,
        String originalPrice,
        String discount,
        double rating,
        int reviewCount,
        String store,
        IconType artwork,
        String artworkClass,
        String imageUrl,
        String description,
        boolean saved,
        List<ProductOffer> offers,
        String externalUrl
) {
    public Product {
        offers = List.copyOf(offers == null ? List.of() : offers);
        externalUrl = normalizeExternalUrl(externalUrl);
    }

    /**
     * Backwards-compatible constructor for callers using the offer-aware model.
     * A known offer URL is also retained as the product's primary external URL.
     */
    public Product(
            String id,
            String brand,
            String name,
            String category,
            String price,
            String originalPrice,
            String discount,
            double rating,
            int reviewCount,
            String store,
            IconType artwork,
            String artworkClass,
            String imageUrl,
            String description,
            boolean saved,
            List<ProductOffer> offers
    ) {
        this(id, brand, name, category, price, originalPrice, discount, rating, reviewCount, store,
                artwork, artworkClass, imageUrl, description, saved, offers, firstOfferUrl(offers));
    }

    public Product(
            String brand,
            String name,
            String price,
            String originalPrice,
            String discount,
            double rating,
            int reviewCount,
            String store,
            IconType artwork,
            String artworkClass,
            boolean saved
    ) {
        this(brand, name, price, originalPrice, discount, rating, reviewCount, store,
                artwork, artworkClass, null, saved);
    }

    public Product(
            String brand,
            String name,
            String price,
            String originalPrice,
            String discount,
            double rating,
            int reviewCount,
            String store,
            IconType artwork,
            String artworkClass,
            String imageUrl,
            boolean saved
    ) {
        this(stableId(brand, name), brand, name, null, price, originalPrice, discount,
                rating, reviewCount, store, artwork, artworkClass, imageUrl, null, saved, List.of());
    }

    /**
     * Backwards-compatible constructor for callers that use the pre-offer model shape.
     */
    public Product(
            String id,
            String brand,
            String name,
            String category,
            String price,
            String originalPrice,
            String discount,
            double rating,
            int reviewCount,
            String store,
            IconType artwork,
            String artworkClass,
            String imageUrl,
            String description,
            boolean saved
    ) {
        this(id, brand, name, category, price, originalPrice, discount, rating, reviewCount, store,
                artwork, artworkClass, imageUrl, description, saved, List.of());
    }

    private static String stableId(String brand, String name) {
        String value = (brand + "-" + name).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return value.isBlank() ? "product" : value;
    }

    private static String firstOfferUrl(List<ProductOffer> offers) {
        if (offers == null) {
            return null;
        }
        return offers.stream().filter(java.util.Objects::nonNull)
                .map(ProductOffer::productUrl).findFirst().orElse(null);
    }

    private static String normalizeExternalUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String candidate = value.trim();
        try {
            java.net.URI uri = java.net.URI.create(candidate);
            if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null) {
                return candidate;
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid provider URLs are omitted rather than exposed or opened.
        }
        return null;
    }
}
