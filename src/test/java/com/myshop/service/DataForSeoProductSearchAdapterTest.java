package com.myshop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.config.DataForSeoConfig;
import com.myshop.model.Product;
import com.myshop.model.SearchFilters;
import com.myshop.model.SearchRequest;
import com.myshop.model.SearchSortMode;
import com.myshop.model.ShoppingIntent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataForSeoProductSearchAdapterTest {

    @Test
    void mapsLiveProductFieldsAndCreatesOffer() {
        FakeTransport transport = new FakeTransport(
                new DataForSeoHttpResponse(200, taskCreated()),
                new DataForSeoHttpResponse(200, completed(
                        """
                        {"type":"google_shopping_serp","title":"White sneakers","seller":"Campus Store","price":3499,"old_price":4999,"currency":"INR","product_id":"product-1","description":"Comfortable college sneakers","availability":"in_stock","product_rating":{"value":4.6,"rating_max":5,"votes_count":218},"reviews_count":218,"product_images":["https://images.example/sneaker.jpg"],"shopping_url":"https://shopping.google.com/product-1"}
                        """))
        );
        DataForSeoProductSearchAdapter adapter = new DataForSeoProductSearchAdapter(
                config(), transport, new ObjectMapper()
        );

        Product product = adapter.search(request(5000)).getFirst();

        assertEquals("product-1", product.id());
        assertEquals("White sneakers", product.name());
        assertEquals("Footwear", product.category());
        assertEquals("₹3499", product.price());
        assertEquals("₹4999", product.originalPrice());
        assertEquals("30% off", product.discount());
        assertEquals(4.6, product.rating());
        assertEquals(218, product.reviewCount());
        assertEquals("Campus Store", product.store());
        assertEquals("https://images.example/sneaker.jpg", product.imageUrl());
        assertEquals(1, product.offers().size());
        assertEquals(new BigDecimal("3499"), product.offers().getFirst().price());
        assertEquals("https://shopping.google.com/product-1", product.offers().getFirst().productUrl());
        assertEquals(com.myshop.model.ProductOffer.Availability.IN_STOCK,
                product.offers().getFirst().availability());
    }

    @Test
    void filtersClearlyOverBudgetProductsButKeepsUnpricedProducts() {
        FakeTransport transport = new FakeTransport(
                new DataForSeoHttpResponse(200, taskCreated()),
                new DataForSeoHttpResponse(200, completed(
                        """
                        {"type":"google_shopping_serp","title":"Over budget","seller":"Store","price":6000,"currency":"INR","product_id":"over","shopping_url":"https://store.example/over"}
                        """,
                        """
                        {"type":"google_shopping_serp","title":"Price unavailable","seller":"Store","product_id":"unknown","shopping_url":"https://store.example/unknown"}
                        """))
        );

        List<Product> products = new DataForSeoProductSearchAdapter(config(), transport, new ObjectMapper())
                .search(request(5000));

        assertEquals(List.of("unknown"), products.stream().map(Product::id).toList());
        assertTrue(products.getFirst().price().isBlank());
    }

    @Test
    void malformedItemsAreIgnoredAndInvalidUrlsDoNotCreateOffers() {
        FakeTransport transport = new FakeTransport(
                new DataForSeoHttpResponse(200, taskCreated()),
                new DataForSeoHttpResponse(200, completed(
                        """
                        {"type":"google_shopping_serp","title":"","price":10}
                        """,
                        """
                        {"type":"google_shopping_serp","title":"Valid product","seller":"Store","price":10,"currency":"INR","product_id":"valid","url":"not-a-url"}
                        """))
        );

        Product product = new DataForSeoProductSearchAdapter(config(), transport, new ObjectMapper())
                .search(request(null)).getFirst();

        assertEquals("valid", product.id());
        assertTrue(product.offers().isEmpty());
        assertFalse(product.name().isBlank());
    }

    @Test
    void requestUsesCurrentQueryAndBudgetWithoutPersistedPreferenceDump() {
        FakeTransport transport = new FakeTransport(
                new DataForSeoHttpResponse(200, taskCreated()),
                new DataForSeoHttpResponse(200, completed(
                        "{\"type\":\"google_shopping_serp\",\"title\":\"Product\",\"product_id\":\"p\"}"))
        );
        new DataForSeoProductSearchAdapter(config(), transport, new ObjectMapper()).search(request(5000));

        String submitted = transport.requests.getFirst();
        assertTrue(submitted.contains("white sneakers under ₹5000 for college"));
        assertTrue(submitted.contains("\"price_max\":5000.0"));
        assertFalse(submitted.contains("favorite"));
    }

    @Test
    void missingDataForSeoCredentialsSelectsMockProvider() {
        assertTrue(ProductSearchProviderFactory.create(
                new DataForSeoConfig("", "", null, null, null,
                        DataForSeoConfig.DEFAULT_REQUEST_TIMEOUT,
                        DataForSeoConfig.DEFAULT_POLL_INTERVAL,
                        DataForSeoConfig.DEFAULT_POLL_TIMEOUT,
                        DataForSeoConfig.DEFAULT_MAX_RESULTS)
        ) instanceof MockProductSearchProvider);
    }

    @Test
    void liveProviderFallsBackToMockWhenAdapterFails() {
        LiveProductSearchProvider provider = new LiveProductSearchProvider(
                new com.myshop.config.ProductSearchProviderConfig(
                        "dataforseo", "configured", null,
                        com.myshop.config.ProductSearchProviderConfig.DEFAULT_REQUEST_TIMEOUT
                ),
                ignored -> { throw new IllegalStateException("network failure"); },
                new MockProductSearchProvider()
        );

        List<Product> products = provider.search(new SearchRequest(
                "headphones", null, 1, null, SearchFilters.none(), SearchSortMode.RECOMMENDED
        ));

        assertEquals(List.of("marshall-major-v-headphones"), products.stream().map(Product::id).toList());
    }

    private DataForSeoConfig config() {
        return new DataForSeoConfig(
                "login", "password", URI.create("https://api.example.test"),
                "India", "English", Duration.ofSeconds(1), Duration.ZERO,
                Duration.ofSeconds(1), 20
        );
    }

    private SearchRequest request(Integer maxBudget) {
        return new SearchRequest(
                "white sneakers under ₹5000 for college", null, 1, null,
                SearchFilters.none(), SearchSortMode.RECOMMENDED,
                new ShoppingIntent("white sneakers under ₹5000 for college", "Footwear", "Sneakers",
                        null, maxBudget == null ? null : maxBudget.doubleValue(), List.of(),
                        List.of("white"), List.of("college"), List.of("comfort"), List.of("white sneakers"), null)
        );
    }

    private String taskCreated() {
        return """
                {"status_code":20000,"tasks":[{"id":"task-1","status_code":20100,"status_message":"Task Created."}]}
                """;
    }

    private String completed(String... items) {
        return "{\"status_code\":20000,\"tasks\":[{\"status_code\":20000,\"result\":[{\"items\":["
                + String.join(",", items) + "]}]}]}";
    }

    private static final class FakeTransport implements DataForSeoTransport {
        private final Queue<DataForSeoHttpResponse> responses = new ArrayDeque<>();
        private final List<String> requests = new ArrayList<>();

        private FakeTransport(DataForSeoHttpResponse... responses) {
            this.responses.addAll(List.of(responses));
        }

        @Override
        public DataForSeoHttpResponse send(String method, URI uri, String login, String password,
                                           String body, Duration timeout) {
            requests.add(body);
            return responses.remove();
        }
    }
}
