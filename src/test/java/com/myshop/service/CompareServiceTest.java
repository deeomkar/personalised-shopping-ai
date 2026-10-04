package com.myshop.service;

import com.myshop.component.IconType;
import com.myshop.model.Product;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompareServiceTest {
    @Test
    void compareHasThreeItemCapAndRejectsDuplicates() {
        CompareService service = new CompareService();
        assertTrue(service.add(product("1"))); assertTrue(service.add(product("2"))); assertTrue(service.add(product("3")));
        assertFalse(service.add(product("3"))); assertFalse(service.add(product("4"))); assertEquals(3, service.products().size());
        assertTrue(service.remove("2")); assertTrue(service.add(product("4"))); service.clear(); assertTrue(service.products().isEmpty());
    }

    @Test
    void toggleAddsAndRemoves() {
        CompareService service = new CompareService(); Product product = product("1");
        assertTrue(service.toggle(product)); assertTrue(service.contains("1")); assertFalse(service.toggle(product)); assertFalse(service.contains("1"));
    }

    private Product product(String id) { return new Product(id, "Brand", "Product " + id, "Footwear", "₹1,000", null, null, 4, 1, "Store", IconType.SHOE, "artwork-sage", null, null, false); }
}
