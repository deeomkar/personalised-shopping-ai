package com.myshop.service;

import com.myshop.component.IconType;
import com.myshop.model.Product;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductTypeRelevanceTest {

    @Test
    void extractsCoreTypeBeforeUseCasePhrase() {
        assertEquals("sunscreen", ProductTypeRelevance.coreProductType("sunscreen for oily skin"));
        assertEquals("moisturizer", ProductTypeRelevance.coreProductType("moisturizer under 1000"));
    }

    @Test
    void matchesSynonymsAcrossProductFields() {
        Product product = product("uv-shield", "SunCare", "Daily UV Shield", "Beauty",
                "SPF 50 broad spectrum protection for oily skin");

        assertTrue(ProductTypeRelevance.matches(product, "sunscreen for oily skin"));
        assertEquals(List.of("uv-shield"), ProductTypeRelevance.filter(
                "sunscreen", List.of(product, product("cleanser", "Care", "Foaming Cleanser", "Beauty", "Face wash"))
        ).stream().map(Product::id).toList());
    }

    @Test
    void unknownProductTypesUseGenericTextMatching() {
        List<String> types = List.of(
                "lipstick", "shampoo", "laptop", "handbag", "perfume", "keyboard", "mouse",
                "kurta", "jeans", "conditioner", "power bank"
        );
        for (String type : types) {
            Product match = product(type, "Brand", type, "General", "Useful " + type + " product");
            assertEquals(List.of(type), ProductTypeRelevance.filter(type, List.of(match))
                    .stream().map(Product::id).toList(), "type=" + type);
        }
    }

    @Test
    void unknownTypesDoNotFailClosedWhenProviderVocabularyDiffers() {
        List<Product> providerResults = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            providerResults.add(product("result-" + index, "Brand", "Provider result " + index,
                    "Electronics", "Popular product"));
        }

        assertEquals(20, ProductTypeRelevance.filter("laptop", providerResults).size());
    }

    private Product product(String id, String brand, String name, String category, String description) {
        return new Product(id, brand, name, category, "₹999", null, null, 4.2, 10,
                "Store", IconType.BAG, "artwork", null, description, false);
    }
}
