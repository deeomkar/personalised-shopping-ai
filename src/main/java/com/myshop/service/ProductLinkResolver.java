package com.myshop.service;

import com.myshop.model.Product;
import com.myshop.model.ProductOffer;

import java.net.URI;
import java.util.Locale;

/** Resolves the one safe, user-facing destination shown by Product Details. */
public final class ProductLinkResolver {
    private ProductLinkResolver() { }

    public static String resolve(Product product) {
        if (product == null) {
            return null;
        }
        if (ExternalLinkService.isSafe(product.externalUrl())) {
            return product.externalUrl();
        }
        return product.offers().stream()
                .map(ProductOffer::productUrl)
                .filter(ExternalLinkService::isSafe)
                .findFirst()
                .orElse(null);
    }

    public static String actionLabel(String url) {
        return isGoogleShoppingUrl(url) ? "View product" : "Visit store";
    }

    private static boolean isGoogleShoppingUrl(String url) {
        if (!ExternalLinkService.isSafe(url)) {
            return false;
        }
        try {
            String host = URI.create(url).getHost();
            return host != null && host.toLowerCase(Locale.ROOT).contains("google.");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
