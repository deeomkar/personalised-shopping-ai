package com.myshop.service;

import com.myshop.component.IconType;
import com.myshop.model.Product;
import com.myshop.model.ProductOffer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductOfferServiceTest {
    @Test
    void returnsKnownOffersCheapestFirstAndCachesSnapshot() {
        ProductOffer expensive = new ProductOffer("p", "Store B", new BigDecimal("20"), null, "INR", "https://b.test/p", ProductOffer.Availability.UNKNOWN);
        ProductOffer cheap = new ProductOffer("p", "Store A", new BigDecimal("10"), null, "INR", "https://a.test/p", ProductOffer.Availability.IN_STOCK);
        Product product = new Product("p", "Brand", "Product", "Footwear", "₹10", null, null, 4, 1, "Store A", IconType.SHOE, "artwork-live", null, null, false, List.of(expensive, cheap));
        ProductOfferService service = new ProductOfferService();
        assertEquals(List.of(cheap, expensive), service.offersFor(product));
        assertEquals(service.offersFor(product), service.offersFor(product));
    }
}
