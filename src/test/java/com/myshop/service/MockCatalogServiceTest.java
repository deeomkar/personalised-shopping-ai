package com.myshop.service;

import com.myshop.model.Product;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MockCatalogServiceTest {

    @Test
    void fallbackCatalogUsesIndiaSafePricesAndNoDemoStores() {
        var products = MockCatalogService.searchableProducts();

        assertTrue(products.stream().allMatch(product -> product.price().startsWith("₹")));
        assertTrue(products.stream().allMatch(product -> product.store() == null));
    }

    @Test
    void discoverPlaceholdersHaveNoPurchasableMetadata() {
        assertTrue(MockCatalogService.discoveryPlaceholders().stream()
                .allMatch(product -> product.price() == null && product.store() == null
                        && product.offers().isEmpty()));
    }
}
