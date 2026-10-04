package com.myshop.service;

import com.myshop.model.Product;
import com.myshop.model.ProductOffer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Provider-neutral offer seam. Current SerpApi offers are already on Product. */
public final class ProductOfferService {
    private final Map<String, List<ProductOffer>> cache = new ConcurrentHashMap<>();

    public List<ProductOffer> offersFor(Product product) {
        if (product == null) return List.of();
        return cache.computeIfAbsent(product.id(), ignored -> product.offers().stream()
                .filter(offer -> offer != null).distinct()
                .sorted(java.util.Comparator.comparing(ProductOffer::price)).toList());
    }

    public void clear() { cache.clear(); }
}
