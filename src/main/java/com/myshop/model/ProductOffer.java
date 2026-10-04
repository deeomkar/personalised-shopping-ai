package com.myshop.model;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;

/**
 * A concrete store offer for a normalized product.
 *
 * <p>An offer is only valid when the upstream source provides a price and a
 * direct product URL. The model deliberately does not create fallback URLs
 * or prices.</p>
 */
public record ProductOffer(
        String productId,
        String storeName,
        BigDecimal price,
        BigDecimal originalPrice,
        String currency,
        String productUrl,
        Availability availability,
        String delivery
) {
    public ProductOffer {
        productId = requireText(productId, "productId");
        storeName = requireText(storeName, "storeName");
        price = requireNonNegative(price, "price");
        originalPrice = originalPrice == null ? null : requireNonNegative(originalPrice, "originalPrice");
        currency = normalizeCurrency(currency);
        productUrl = requireHttpUrl(productUrl);
        availability = availability == null ? Availability.UNKNOWN : availability;
        delivery = delivery == null || delivery.isBlank() ? null : delivery.trim();
    }

    /** Backwards-compatible offer constructor without delivery metadata. */
    public ProductOffer(
            String productId,
            String storeName,
            BigDecimal price,
            BigDecimal originalPrice,
            String currency,
            String productUrl,
            Availability availability
    ) {
        this(productId, storeName, price, originalPrice, currency, productUrl, availability, null);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }

    private static String normalizeCurrency(String value) {
        String normalized = requireText(value, "currency").toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currency must be a 3-letter ISO code");
        }
        return normalized;
    }

    private static String requireHttpUrl(String value) {
        String normalized = requireText(value, "productUrl");
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("productUrl must be a valid HTTP(S) URL", exception);
        }
        if (!("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalArgumentException("productUrl must be a valid HTTP(S) URL");
        }
        return normalized;
    }

    public enum Availability {
        IN_STOCK,
        LIMITED_STOCK,
        OUT_OF_STOCK,
        BACKORDERED,
        PRE_ORDER,
        UNKNOWN
    }
}
