package com.myshop.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductOfferTest {

    @Test
    void normalizesCurrencyAndDefaultsAvailability() {
        ProductOffer offer = new ProductOffer(
                "sku-1", "Example Store", new BigDecimal("999.00"), null,
                "inr", "https://example.invalid/products/sku-1", null
        );

        assertEquals("INR", offer.currency());
        assertEquals(ProductOffer.Availability.UNKNOWN, offer.availability());
    }

    @Test
    void rejectsOfferWithoutDirectHttpProductUrl() {
        assertThrows(IllegalArgumentException.class, () -> new ProductOffer(
                "sku-1", "Example Store", BigDecimal.TEN, null,
                "INR", "not-a-product-url", ProductOffer.Availability.IN_STOCK
        ));
    }

    @Test
    void productKeepsOffersImmutableWithoutChangingExistingConstruction() {
        ProductOffer offer = new ProductOffer(
                "sku-1", "Example Store", BigDecimal.TEN, null,
                "INR", "https://example.invalid/products/sku-1", ProductOffer.Availability.IN_STOCK
        );
        Product product = new Product(
                "product-1", "Example", "Example Product", "Electronics",
                "₹10", null, null, 4.0, 1, "Example Store", null, null, null, null,
                false, List.of(offer)
        );

        assertEquals(List.of(offer), product.offers());
        assertThrows(UnsupportedOperationException.class, () -> product.offers().add(offer));
    }
}
