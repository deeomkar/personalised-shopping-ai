package com.myshop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.config.ProductSearchProviderConfig;
import com.myshop.config.SerpApiConfig;
import com.myshop.model.Product;
import com.myshop.model.ProductOffer;
import com.myshop.model.SearchFilters;
import com.myshop.model.SearchRequest;
import com.myshop.model.SearchResult;
import com.myshop.model.SearchSortMode;
import com.myshop.model.ShoppingIntent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerpApiProductSearchAdapterTest {

    @Test
    void mapsShoppingResultFieldsAndCreatesOffer() {
        FakeTransport transport = new FakeTransport(new SerpApiHttpResponse(200, """
                {
                  "shopping_results": [{
                    "title": "White sneakers",
                    "product_id": "serp-product-1",
                    "source": "Campus Store",
                    "price": "₹3,499",
                    "extracted_price": 3499,
                    "old_price": "₹4,999",
                    "extracted_old_price": 4999,
                    "rating": 4.6,
                    "reviews": 218,
                    "thumbnail": "https://images.example/sneakers.webp",
                    "link": "https://store.example/sneakers",
                    "product_link": "https://www.google.com/shopping/product/1",
                    "delivery": "Get it by tomorrow"
                  }]
                }
                """));
        SerpApiProductSearchAdapter adapter = new SerpApiProductSearchAdapter(
                config(), transport, new ObjectMapper()
        );

        Product product = adapter.search(request(5000)).getFirst();
        ProductOffer offer = product.offers().getFirst();

        assertEquals("serp-product-1", product.id());
        assertEquals("White sneakers", product.name());
        assertEquals("Footwear", product.category());
        assertEquals("₹3,499", product.price());
        assertEquals("₹4,999", product.originalPrice());
        assertEquals("30% off", product.discount());
        assertEquals(4.6, product.rating());
        assertEquals(218, product.reviewCount());
        assertEquals("Campus Store", product.store());
        assertEquals("https://images.example/sneakers.webp", product.imageUrl());
        assertEquals("https://store.example/sneakers", product.externalUrl());
        assertEquals(new BigDecimal("3499"), offer.price());
        assertEquals("INR", offer.currency());
        assertEquals("https://store.example/sneakers", offer.productUrl());
        assertEquals("Get it by tomorrow", offer.delivery());
    }

    @Test
    void buildsConciseIndiaQueryAndBudgetParameter() {
        FakeTransport transport = new FakeTransport(new SerpApiHttpResponse(200, "{\"shopping_results\":[]}"));
        new SerpApiProductSearchAdapter(config(), transport, new ObjectMapper()).search(request(5000));

        Map<String, String> query = queryParameters(transport.lastUri);
        assertEquals("google_shopping", query.get("engine"));
        assertEquals("white sneakers under 5000", query.get("q"));
        assertEquals("test-key", query.get("api_key"));
        assertFalse(query.get("api_key").isBlank());
        assertEquals("en", query.get("hl"));
        assertEquals("in", query.get("gl"));
        assertFalse(query.containsKey("max_price"));
        assertFalse(transport.lastUri.toString().contains("%2520"));
        assertFalse(transport.lastUri.toString().contains("%22"));
        assertFalse(transport.lastUri.toString().contains("favorite"));
    }

    @Test
    void filtersOverBudgetProductsAndIgnoresMalformedItems() {
        FakeTransport transport = new FakeTransport(new SerpApiHttpResponse(200, """
                {"shopping_results":[
                  {"title":"","price":"₹100"},
                  {"title":"Too expensive","product_id":"expensive","price":"₹6,000","extracted_price":6000,"source":"Store","link":"https://store.example/expensive"},
                  {"title":"Unknown price","product_id":"unknown","source":"Store","link":"not-a-url"}
                ]}
                """));

        List<Product> products = new SerpApiProductSearchAdapter(config(), transport, new ObjectMapper())
                .search(request(5000));

        assertEquals(List.of("unknown"), products.stream().map(Product::id).toList());
        assertTrue(products.getFirst().offers().isEmpty());
    }

    @Test
    void preservesProductLinkWhenMerchantLinkIsMissing() {
        FakeTransport transport = new FakeTransport(new SerpApiHttpResponse(200, """
                {"shopping_results":[{
                  "title":"White sneakers",
                  "product_id":"product-link-only",
                  "price":"₹1,999",
                  "extracted_price":1999,
                  "currency":"INR",
                  "source":"Shopping result",
                  "product_link":"https://www.google.com/shopping/product/2",
                  "thumbnail":"https://images.example/product-link-only.webp"
                }]}
                """));

        Product product = new SerpApiProductSearchAdapter(config(), transport, new ObjectMapper())
                .search(request(5000)).getFirst();

        assertEquals("https://www.google.com/shopping/product/2", product.externalUrl());
        assertEquals("https://www.google.com/shopping/product/2", product.offers().getFirst().productUrl());
        assertEquals("https://images.example/product-link-only.webp", product.imageUrl());
    }

    @Test
    void normalizesProviderProductLinkFormsWithoutInventingMerchantUrls() {
        FakeTransport transport = new FakeTransport(new SerpApiHttpResponse(200, """
                {"shopping_results":[
                  {"title":"Protocol relative","product_id":"relative-1",
                   "product_link":"//www.google.com/shopping/product/3"},
                  {"title":"Path product","product_id":"relative-2",
                   "product_link":"/shopping/product/4"}
                ]}
                """));

        List<Product> products = new SerpApiProductSearchAdapter(config(), transport, new ObjectMapper())
                .search(request(5000));

        assertEquals("https://www.google.com/shopping/product/3", products.get(0).externalUrl());
        assertEquals("https://www.google.co.in/shopping/product/4", products.get(1).externalUrl());
    }

    @Test
    void encodesWhitespaceInGoogleShoppingProductLinkBeforeValidation() {
        FakeTransport transport = new FakeTransport(new SerpApiHttpResponse(200, """
                {"shopping_results":[{
                  "title":"Moisturiser",
                  "product_id":"moisturiser-1",
                  "price":"₹999",
                  "extracted_price":999,
                  "currency":"INR",
                  "source":"Store",
                  "product_link":"https://www.google.co.in/search?q=moisturiser under 1000&rds=foo|bar"
                }]}
                """));

        Product product = new SerpApiProductSearchAdapter(config(), transport, new ObjectMapper())
                .search(request(1000)).getFirst();

        assertEquals("https://www.google.co.in/search?q=moisturiser%20under%201000&rds=foo%7Cbar",
                product.externalUrl());
        assertEquals(1, product.offers().size());
    }

    @Test
    void mapsSerpApiThumbnailFallbacksInDocumentedPriorityOrder() {
        assertEquals("https://images.example/serpapi.webp", mappedImage("""
                {"title":"SerpApi image","product_id":"image-1",
                 "serpapi_thumbnail":"https://images.example/serpapi.webp",
                 "thumbnails":["https://images.example/array.webp"]}
                """));
        assertEquals("https://images.example/array.webp", mappedImage("""
                {"title":"Thumbnail array image","product_id":"image-2",
                 "thumbnails":["https://images.example/array.webp"]}
                """));
        assertEquals("https://images.example/serpapi-array.webp", mappedImage("""
                {"title":"SerpApi thumbnail array image","product_id":"image-3",
                 "serpapi_thumbnails":["https://images.example/serpapi-array.webp"]}
                """));
    }

    @Test
    void preservesImageAndExternalUrlThroughSearchResult() {
        Product product = new Product(
                "product-1", "Brand", "Product", "Footwear", "₹999", null, null,
                4.5, 10, "Store", null, "artwork-live", "https://images.example/product.webp",
                "Description", false, List.of(), "https://store.example/product"
        );
        SearchResult result = SearchResult.success(request(5000), List.of(product));

        assertEquals("https://images.example/product.webp", result.products().getFirst().imageUrl());
        assertEquals("https://store.example/product", result.products().getFirst().externalUrl());
    }

    private String mappedImage(String item) {
        FakeTransport transport = new FakeTransport(new SerpApiHttpResponse(200,
                "{\"shopping_results\":[" + item + "]}"));
        return new SerpApiProductSearchAdapter(config(), transport, new ObjectMapper())
                .search(request(5000)).getFirst().imageUrl();
    }

    @Test
    void missingKeySelectsMockProvider() {
        SerpApiConfig missing = new SerpApiConfig(
                "", null, null, null, null,
                SerpApiConfig.DEFAULT_REQUEST_TIMEOUT, SerpApiConfig.DEFAULT_MAX_RESULTS
        );
        assertTrue(ProductSearchProviderFactory.create(missing) instanceof MockProductSearchProvider);
    }

    @Test
    void factoryCarriesTheConfiguredKeyIntoTheLiveProviderConfiguration() {
        SerpApiConfig config = config();

        ProductSearchProvider provider = ProductSearchProviderFactory.create(config);

        assertTrue(provider instanceof LiveProductSearchProvider);
        assertEquals(config.apiKey(), ((LiveProductSearchProvider) provider).configuration().apiKey());
    }

    @Test
    void apiFailureFallsBackToMockProvider() {
        SerpApiProductSearchAdapter adapter = new SerpApiProductSearchAdapter(
                config(), new FakeTransport(new SerpApiHttpResponse(401, "{\"error\":\"invalid key\"}")),
                new ObjectMapper()
        );
        LiveProductSearchProvider provider = new LiveProductSearchProvider(
                new ProductSearchProviderConfig("serpapi", "configured", null,
                        ProductSearchProviderConfig.DEFAULT_REQUEST_TIMEOUT),
                adapter, new MockProductSearchProvider()
        );

        List<Product> products = provider.search(new SearchRequest(
                "headphones", null, 1, null, SearchFilters.none(), SearchSortMode.RECOMMENDED
        ));

        assertEquals(List.of("marshall-major-v-headphones"), products.stream().map(Product::id).toList());
    }

    @Test
    void capturesRedactedDiagnosticsForHttpFailure() {
        SerpApiProductSearchAdapter adapter = new SerpApiProductSearchAdapter(
                config(), new FakeTransport(new SerpApiHttpResponse(
                        401, "{\"error\":\"Invalid API key test-key\",\"api_key\":\"test-key\"}"
                )), new ObjectMapper()
        );

        SerpApiProductSearchAdapter.SerpApiException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        SerpApiProductSearchAdapter.SerpApiException.class,
                        () -> adapter.search(request(5000))
                );
        SerpApiDiagnostics diagnostics = exception.diagnostics();

        assertEquals(401, diagnostics.statusCode());
        assertEquals("https://serpapi.example/search.json", diagnostics.endpoint());
        assertTrue(diagnostics.googleShoppingEngine());
        assertTrue(diagnostics.queryPresent());
        assertTrue(diagnostics.apiKeyPresentAndNonEmpty());
        assertTrue(diagnostics.englishLanguage());
        assertTrue(diagnostics.indiaCountry());
        assertTrue(diagnostics.providerMessage().contains("Invalid API key"));
        assertFalse(diagnostics.sanitizedResponseBody().contains("test-key"));
    }

    @Test
    void capturesProviderErrorAndSearchMetadataForHttp200() {
        SerpApiProductSearchAdapter adapter = new SerpApiProductSearchAdapter(
                config(), new FakeTransport(new SerpApiHttpResponse(
                        200, "{\"search_metadata\":{\"id\":\"search-123\"},"
                                + "\"error\":\"Google Shopping unavailable\"}"
                )), new ObjectMapper()
        );

        SerpApiProductSearchAdapter.SerpApiException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        SerpApiProductSearchAdapter.SerpApiException.class,
                        () -> adapter.search(request(5000))
                );
        SerpApiDiagnostics diagnostics = exception.diagnostics();

        assertEquals(200, diagnostics.statusCode());
        assertEquals("PROVIDER_ERROR", diagnostics.responseCategory());
        assertEquals("Google Shopping unavailable", diagnostics.providerMessage());
        assertEquals("search-123", diagnostics.searchId());
        assertFalse(diagnostics.shoppingResultsPresent());
        assertEquals(0, diagnostics.shoppingResultCount());
        assertEquals(0, diagnostics.mappedProductCount());
    }

    private SerpApiConfig config() {
        return new SerpApiConfig(
                "test-key", URI.create("https://serpapi.example/search.json"),
                "in", "en", "google.co.in", Duration.ofSeconds(1), 20
        );
    }

    private SearchRequest request(double maxBudget) {
        return new SearchRequest(
                "white sneakers under 5000", null, 1, null, SearchFilters.none(),
                SearchSortMode.RECOMMENDED,
                new ShoppingIntent("white sneakers under 5000", "Footwear", "Sneakers",
                        null, maxBudget, List.of(), List.of("white"), List.of("college"),
                        List.of("comfort"), List.of("white sneakers"), null)
        );
    }

    private Map<String, String> queryParameters(URI uri) {
        Map<String, String> values = new LinkedHashMap<>();
        String rawQuery = uri.getRawQuery();
        for (String parameter : rawQuery.split("&")) {
            String[] parts = parameter.split("=", 2);
            values.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
            );
        }
        return values;
    }

    private static final class FakeTransport implements SerpApiTransport {
        private final SerpApiHttpResponse response;
        private URI lastUri;

        private FakeTransport(SerpApiHttpResponse response) {
            this.response = response;
        }

        @Override
        public SerpApiHttpResponse get(URI uri, Duration timeout) {
            lastUri = uri;
            return response;
        }
    }
}
