package com.myshop.service;

import com.myshop.model.Product;
import com.myshop.model.SearchFilters;
import com.myshop.model.SearchRequest;
import com.myshop.model.SearchResult;
import com.myshop.model.SearchSortMode;
import com.myshop.model.UserPreference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchServiceTest {

    private final SearchService searchService = new SearchService(new MockProductSearchProvider());

    @Test
    void searchRequestNormalizesQueryAndUsesUsefulDefaults() {
        SearchRequest request = new SearchRequest("  shoes  ", null, 42, null, null, null);

        assertEquals("shoes", request.rawQuery());
        assertEquals(SearchFilters.none(), request.filters());
        assertEquals(SearchSortMode.RECOMMENDED, request.sortMode());
        assertTrue(request.hasSearchIntent());
    }

    @Test
    void mockProviderReturnsDeterministicProducts() {
        SearchRequest request = request("headphones");

        List<String> first = ids(searchService.search(request));
        List<String> second = ids(searchService.search(request));

        assertEquals(first, second);
        assertEquals(List.of("marshall-major-v-headphones"), first);
    }

    @Test
    void queryAndCategoryFilteringUseTheSamePipeline() {
        SearchResult query = searchService.search(request("footwear"));
        SearchResult category = searchService.search(new SearchRequest(
                "", "Footwear", 42, null, SearchFilters.none(), SearchSortMode.RECOMMENDED
        ));

        assertEquals(List.of("on-running-cloud-5-in-chalk", "veja-campo-leather-sneakers"), ids(query));
        assertEquals(2, category.totalResultCount());
        assertTrue(category.products().stream().allMatch(product -> "Footwear".equals(product.category())));
    }

    @Test
    void priceFilteringWorks() {
        SearchResult result = searchService.search(new SearchRequest(
                "", null, 42, null,
                new SearchFilters(null, null, 100.0, 160.0, 0),
                SearchSortMode.PRICE_LOW_TO_HIGH
        ));

        assertEquals(List.of("veja-campo-leather-sneakers", "marshall-major-v-headphones",
                "on-running-cloud-5-in-chalk"), ids(result));
    }

    @Test
    void ratingFilteringWorks() {
        SearchResult result = searchService.search(new SearchRequest(
                "", null, 42, null,
                new SearchFilters(null, null, null, null, 4.8),
                SearchSortMode.RATING
        ));

        assertTrue(result.products().stream().allMatch(product -> product.rating() >= 4.8));
        assertEquals(3, result.totalResultCount());
    }

    @Test
    void lowToHighSortWorks() {
        SearchResult result = searchService.search(new SearchRequest(
                "", null, 42, null, SearchFilters.none(), SearchSortMode.PRICE_LOW_TO_HIGH
        ));

        assertEquals(List.of("aesop-resurrection-aromatique", "ferm-living-ripple-glass-set",
                "uniqlo-soft-ribbed-overshirt"), ids(result).subList(0, 3));
    }

    @Test
    void highToLowSortWorks() {
        SearchResult result = searchService.search(new SearchRequest(
                "", null, 42, null, SearchFilters.none(), SearchSortMode.PRICE_HIGH_TO_LOW
        ));

        assertEquals("seiko-presage-cocktail-time", ids(result).getFirst());
    }

    @Test
    void ratingSortWorks() {
        SearchResult result = searchService.search(new SearchRequest(
                "", null, 42, null, SearchFilters.none(), SearchSortMode.RATING
        ));

        assertEquals("seiko-presage-cocktail-time", ids(result).getFirst());
    }

    @Test
    void preferenceAwareRankingGivesFavouriteBrandABonus() {
        UserPreference preference = new UserPreference(
                42, Set.of("Beauty"), Set.of("Quality"),
                UserPreference.ShoppingStyle.NO_PREFERENCE, "Aesop", true, Instant.now()
        );
        SearchResult result = searchService.search(new SearchRequest(
                "", null, 42, preference, SearchFilters.none(), SearchSortMode.RECOMMENDED
        ));

        assertEquals("aesop-resurrection-aromatique", result.products().getFirst().id());
    }

    @Test
    void emptyResultsHaveAnExplicitEmptyState() {
        SearchResult result = searchService.search(request("something-that-does-not-exist"));

        assertTrue(result.isEmpty());
        assertEquals(0, result.totalResultCount());
        assertTrue(result.products().isEmpty());
    }

    private SearchRequest request(String query) {
        return new SearchRequest(query, null, 42, null, SearchFilters.none(), SearchSortMode.RECOMMENDED);
    }

    private List<String> ids(SearchResult result) {
        return result.products().stream().map(Product::id).toList();
    }
}
