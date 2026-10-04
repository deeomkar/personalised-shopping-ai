package com.myshop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.config.GroqConfig;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroqShoppingIntentProviderTest {

    @Test
    void groqStructuredResponseMapsToShoppingIntent() {
        GroqShoppingIntentProvider provider = provider(new GroqHttpResponse(200, responseJson()),
                new FallbackShoppingIntentProvider());

        ShoppingIntent intent = provider.understand(request("white sneakers under ₹5000"));

        assertEquals("Footwear", intent.category());
        assertEquals("Sneakers", intent.productType());
        assertEquals(5000D, intent.maxBudget());
        assertEquals(List.of("white"), intent.colors());
        assertTrue(intent.priorities().contains("comfort"));
    }

    @Test
    void missingGroqKeyUsesFallback() {
        ShoppingIntent expected = ShoppingIntent.empty("headphones");
        GroqShoppingIntentProvider provider = new GroqShoppingIntentProvider(
                new GroqConfig(null, null), request -> expected,
                new ShoppingIntentJsonMapper(), new ObjectMapper(),
                (apiKey, body) -> { throw new AssertionError("transport must not run"); }
        );

        assertSame(expected, provider.understand(request("headphones")));
    }

    @Test
    void providerFailureFallsThroughToFallback() {
        ShoppingIntent expected = ShoppingIntent.empty("headphones");
        GroqShoppingIntentProvider groq = provider(
                new GroqHttpResponse(429, "rate limited"), request -> expected
        );
        ShoppingIntentProvider fallback = request -> expected;
        ShoppingIntentProviderChain chain = new ShoppingIntentProviderChain(
                groq,
                new GeminiShoppingIntentProvider(new com.myshop.config.GeminiConfig(null, null), request -> expected),
                fallback
        );

        assertSame(expected, chain.understand(request("headphones")));
    }

    @Test
    void malformedResponseFallsThroughToFallback() {
        ShoppingIntent expected = ShoppingIntent.empty("headphones");
        GroqShoppingIntentProvider provider = provider(
                new GroqHttpResponse(200, "{not-json"), request -> expected
        );

        assertSame(expected, provider.understand(request("headphones")));
    }

    @Test
    void searchServiceStillWorksWhenGroqFails() {
        GroqShoppingIntentProvider groq = provider(
                new GroqHttpResponse(503, "service unavailable"), new FallbackShoppingIntentProvider()
        );
        ShoppingIntentProviderChain chain = new ShoppingIntentProviderChain(
                groq,
                new GeminiShoppingIntentProvider(new com.myshop.config.GeminiConfig(null, null), new FallbackShoppingIntentProvider()),
                new FallbackShoppingIntentProvider()
        );
        SearchService service = new SearchService(new MockProductSearchProvider(), chain, null);

        SearchResult result = service.search(request("headphones"));

        assertEquals(List.of("marshall-major-v-headphones"), result.products().stream().map(Product::id).toList());
    }

    @Test
    void currentQueryWinsOverSavedPreferenceContext() {
        UserPreference preference = new UserPreference(
                42, Set.of("Beauty"), Set.of("Price"), UserPreference.ShoppingStyle.BUDGET_CONSCIOUS,
                "Aesop", true, Instant.now()
        );
        GroqShoppingIntentProvider provider = provider(new GroqHttpResponse(200, responseJson()),
                new FallbackShoppingIntentProvider());

        SearchResult result = new SearchService(new MockProductSearchProvider(), provider, null).search(
                new SearchRequest("white sneakers under ₹5000", null, 42, preference,
                        SearchFilters.none(), SearchSortMode.RECOMMENDED)
        );

        assertEquals("Footwear", result.request().category());
        assertTrue(result.products().stream().allMatch(product -> "Footwear".equals(product.category())));
    }

    @Test
    void requestBodyDoesNotContainApiKey() {
        String apiKey = "groq-test-secret";
        AtomicReference<String> capturedBody = new AtomicReference<>();
        GroqShoppingIntentProvider provider = new GroqShoppingIntentProvider(
                new GroqConfig(apiKey, "test-model"), new FallbackShoppingIntentProvider(),
                new ShoppingIntentJsonMapper(), new ObjectMapper(),
                (ignoredKey, body) -> {
                    capturedBody.set(body);
                    return new GroqHttpResponse(200, responseJson());
                }
        );

        provider.understand(request("headphones"));

        assertFalse(capturedBody.get().contains(apiKey));
    }

    @Test
    void defaultModelIsCentralized() {
        assertEquals(GroqConfig.DEFAULT_MODEL, new GroqConfig(null, null).model());
        assertEquals("custom-model", new GroqConfig(null, "custom-model").model());
    }

    private GroqShoppingIntentProvider provider(GroqHttpResponse response, ShoppingIntentProvider fallback) {
        return new GroqShoppingIntentProvider(
                new GroqConfig("test-key", "test-model"), fallback,
                new ShoppingIntentJsonMapper(), new ObjectMapper(),
                (apiKey, body) -> response
        );
    }

    private SearchRequest request(String query) {
        return new SearchRequest(query, null, 42, null, SearchFilters.none(), SearchSortMode.RECOMMENDED);
    }

    private String responseJson() {
        ObjectMapper mapper = new ObjectMapper();
        var intent = mapper.createObjectNode();
        intent.put("category", "Footwear");
        intent.put("productType", "Sneakers");
        intent.putNull("minBudget");
        intent.put("maxBudget", 5000);
        intent.putArray("preferredBrands");
        intent.putArray("colors").add("white");
        intent.putArray("useCases").add("college");
        intent.putArray("priorities").add("comfort").add("minimal style");
        intent.putArray("keywords").add("white sneakers").add("comfortable").add("minimal");
        intent.putNull("shoppingStyle");
        var response = mapper.createObjectNode();
        response.putArray("choices").addObject().putObject("message")
                .put("content", intent.toString());
        return response.toString();
    }
}
