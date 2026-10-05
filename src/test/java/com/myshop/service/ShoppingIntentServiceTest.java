package com.myshop.service;

import com.myshop.config.GeminiConfig;
import com.myshop.model.Product;
import com.myshop.model.SearchFilters;
import com.myshop.model.SearchRequest;
import com.myshop.model.SearchResult;
import com.myshop.model.SearchSortMode;
import com.myshop.model.ShoppingIntent;
import com.myshop.model.UserPreference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShoppingIntentServiceTest {

    @Test
    void structuredJsonMapsToStableIntentModel() {
        SearchRequest request = request("white sneakers under ₹5000");
        ShoppingIntent intent = new ShoppingIntentJsonMapper().map("""
                {
                  "category": "Footwear",
                  "productType": "Sneakers",
                  "minBudget": null,
                  "maxBudget": 5000,
                  "preferredBrands": [],
                  "colors": ["white"],
                  "useCases": ["college"],
                  "priorities": ["comfort", "minimal style"],
                  "keywords": ["white sneakers"],
                  "shoppingStyle": null
                }
                """, request);

        assertEquals("white sneakers under ₹5000", intent.rawQuery());
        assertEquals("Footwear", intent.category());
        assertEquals("Sneakers", intent.productType());
        assertEquals(5000D, intent.maxBudget());
        assertEquals(List.of("white"), intent.colors());
        assertEquals(List.of("comfort", "minimal style"), intent.priorities());
    }

    @Test
    void fallbackParsesUsefulIntentWithoutNetwork() {
        ShoppingIntent intent = new FallbackShoppingIntentProvider().understand(
                request("white sneakers under ₹5,000 for college, comfortable and minimal")
        );

        assertEquals("Footwear", intent.category());
        assertEquals("sneakers", intent.productType());
        assertEquals(5000D, intent.maxBudget());
        assertEquals(List.of("white"), intent.colors());
        assertTrue(intent.useCases().contains("college"));
        assertTrue(intent.priorities().contains("comfortable"));
    }

    @Test
    void fallbackUnderstandsNaturalBeautyIntentAndKBudget() {
        ShoppingIntent intent = new FallbackShoppingIntentProvider().understand(request(
                "moisturizer my skin is dry so I need moisturizer best under 1k and it should be good brands"
        ));

        assertEquals("Beauty", intent.category());
        assertEquals("moisturizer", intent.productType());
        assertEquals(1000D, intent.maxBudget());
        assertTrue(intent.useCases().contains("dry skin"));
        assertEquals("reputable-brands", intent.qualityPreference());
        assertTrue(intent.priorities().contains("best"));
    }

    @Test
    void missingGeminiKeyUsesFallbackProvider() {
        ShoppingIntent expected = ShoppingIntent.empty("headphones");
        ShoppingIntentProvider fallback = request -> expected;
        GeminiShoppingIntentProvider provider = new GeminiShoppingIntentProvider(
                new GeminiConfig(null, null), fallback
        );

        assertSame(expected, provider.understand(request("headphones")));
    }

    @Test
    void providerFailureFallsBackInsideSearchService() {
        SearchService service = new SearchService(
                new MockProductSearchProvider(),
                request -> { throw new IllegalStateException("provider unavailable"); },
                null
        );

        SearchResult result = service.search(request("headphones"));

        assertEquals(List.of("marshall-major-v-headphones"), result.products().stream().map(Product::id).toList());
    }

    @Test
    void malformedStructuredResponseFallsBackInsideSearchService() {
        SearchService service = new SearchService(
                new MockProductSearchProvider(),
                request -> new ShoppingIntentJsonMapper().map("not-json", request),
                null
        );

        SearchResult result = service.search(request("headphones"));

        assertEquals(List.of("marshall-major-v-headphones"), result.products().stream().map(Product::id).toList());
    }

    @Test
    void currentQueryIntentOverridesSavedCategoryAndStyle() {
        UserPreference preference = new UserPreference(
                42, Set.of("Beauty"), Set.of("Price"),
                UserPreference.ShoppingStyle.BUDGET_CONSCIOUS, "Aesop", true, Instant.now()
        );
        ShoppingIntentProvider provider = request -> new ShoppingIntent(
                request.rawQuery(), "Electronics", "Headphones", null, null,
                List.of(), List.of(), List.of(), List.of("sound quality"), List.of("headphones"), "premium"
        );
        SearchService service = new SearchService(new MockProductSearchProvider(), provider, null);

        SearchResult result = service.search(new SearchRequest(
                "headphones", null, 42, preference, SearchFilters.none(), SearchSortMode.RECOMMENDED
        ));

        assertEquals("Electronics", result.request().category());
        assertEquals("premium", result.request().shoppingIntent().shoppingStyle());
        assertTrue(result.products().stream().allMatch(product -> "Electronics".equals(product.category())));
    }

    @Test
    void parsedBudgetIsAppliedToMockProvider() {
        ShoppingIntentProvider provider = request -> new ShoppingIntent(
                request.rawQuery(), null, null, null, 140D,
                List.of(), List.of(), List.of(), List.of(), List.of(), null
        );
        SearchService service = new SearchService(new MockProductSearchProvider(), provider, null);

        SearchResult result = service.search(new SearchRequest(
                "footwear", null, 42, null, SearchFilters.none(), SearchSortMode.RECOMMENDED
        ));

        assertEquals(List.of("veja-campo-leather-sneakers"), result.products().stream().map(Product::id).toList());
        assertEquals(140D, result.request().filters().maximumPrice());
    }

    @Test
    void parsedCategoryIsAppliedToMockProvider() {
        ShoppingIntentProvider provider = request -> new ShoppingIntent(
                request.rawQuery(), "Beauty", null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), null
        );
        SearchService service = new SearchService(new MockProductSearchProvider(), provider, null);

        SearchResult result = service.search(request("beauty"));

        assertEquals("Beauty", result.request().category());
        assertEquals(List.of("aesop-resurrection-aromatique"), result.products().stream().map(Product::id).toList());
    }

    private SearchRequest request(String query) {
        return new SearchRequest(query, null, 42, null, SearchFilters.none(), SearchSortMode.RECOMMENDED);
    }
}
