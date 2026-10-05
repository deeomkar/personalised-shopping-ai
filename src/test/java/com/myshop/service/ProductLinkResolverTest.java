package com.myshop.service;

import com.myshop.model.Product;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductLinkResolverTest {

    @Test
    void resolvesMerchantDestinationAndVisitStoreLabelWhenUrlExists() {
        Product product = product("https://store.example/product");

        assertEquals("https://store.example/product", ProductLinkResolver.resolve(product));
        assertEquals("Visit store", ProductLinkResolver.actionLabel(product.externalUrl()));
    }

    @Test
    void labelsGoogleShoppingDestinationAsViewProduct() {
        Product product = product("https://www.google.com/shopping/product/1");

        assertEquals("View product", ProductLinkResolver.actionLabel(product.externalUrl()));
    }

    @Test
    void hidesDestinationWhenProductUrlIsInvalid() {
        Product product = product("javascript:alert(1)");

        assertNull(ProductLinkResolver.resolve(product));
        assertEquals(ExternalLinkService.Result.INVALID_URL,
                ExternalLinkService.open("javascript:alert(1)"));
    }

    private Product product(String externalUrl) {
        return new Product(
                "product-1", "Brand", "Product", "Footwear", "₹999", null, null,
                4.5, 10, "Store", null, "artwork-live", null, null, false,
                java.util.List.of(), externalUrl
        );
    }
}
