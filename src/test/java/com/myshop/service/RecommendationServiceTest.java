package com.myshop.service;

import com.myshop.component.IconType;
import com.myshop.model.Product;
import com.myshop.model.Recommendation;
import com.myshop.model.RecommendationReason;
import com.myshop.model.SearchFilters;
import com.myshop.model.SearchRequest;
import com.myshop.model.SearchSortMode;
import com.myshop.model.ShoppingIntent;
import com.myshop.model.UserPreference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationServiceTest {

    private final RecommendationService service = new RecommendationService();

    @Test
    void exactProductTypeMatchRanksHigher() {
        List<Recommendation> results = service.rank(request("sneakers", intent("sneakers", "Footwear", "Sneakers"), null),
                List.of(product("sneaker", "Veja", "Campo Sneakers", "Footwear", "₹4,500", 4.2, "White everyday sneaker"),
                        product("shirt", "Uniqlo", "Cotton Shirt", "Fashion", "₹2,000", 4.9, "Soft shirt")));

        assertEquals("sneaker", results.getFirst().product().id());
    }

    @Test
    void explicitCategoryMatchRanksHigher() {
        List<Recommendation> results = service.rank(
                new SearchRequest("", "Footwear", 1, null, SearchFilters.none(), SearchSortMode.RECOMMENDED),
                List.of(product("home", "Ferm", "Glass Set", "Home", "₹2,000", 4.9, "Rippled glass"),
                        product("shoe", "Veja", "Campo", "Footwear", "₹6,000", 4.0, "Leather sneaker")));

        assertEquals("shoe", results.getFirst().product().id());
    }

    @Test
    void withinBudgetRanksAboveOtherwiseEquivalentOverBudgetItem() {
        ShoppingIntent intent = new ShoppingIntent("sneakers under ₹5000", "Footwear", "Sneakers",
                null, 5000D, List.of(), List.of(), List.of(), List.of(), List.of("sneakers"));
        List<Recommendation> results = service.rank(request("sneakers under ₹5000", intent, null),
                List.of(product("over", "Veja", "White Sneakers", "Footwear", "₹6,500", 4.9, "White sneaker"),
                        product("under", "Veja", "White Sneakers", "Footwear", "₹4,500", 4.2, "White sneaker")));

        assertEquals("under", results.getFirst().product().id());
        assertTrue(results.getFirst().reasons().stream().anyMatch(reason -> reason.type() == RecommendationReason.Type.BUDGET));
    }

    @Test
    void explicitBrandPreferenceAffectsScore() {
        ShoppingIntent intent = new ShoppingIntent("nike sneakers", "Footwear", "Sneakers", null, null,
                List.of("Nike"), List.of(), List.of(), List.of(), List.of("sneakers"));
        List<Recommendation> results = service.rank(request("nike sneakers", intent, null),
                List.of(product("adidas", "Adidas", "Forum Sneakers", "Footwear", "₹4,500", 4.8, "Sneakers"),
                        product("nike", "Nike", "Court Sneakers", "Footwear", "₹4,500", 4.2, "Sneakers")));

        assertEquals("nike", results.getFirst().product().id());
    }

    @Test
    void explicitColorTermAffectsScore() {
        ShoppingIntent intent = new ShoppingIntent("white sneakers", "Footwear", "Sneakers", null, null,
                List.of(), List.of("white"), List.of(), List.of(), List.of("white", "sneakers"));
        List<Recommendation> results = service.rank(request("white sneakers", intent, null),
                List.of(product("black", "Veja", "Black Sneakers", "Footwear", "₹4,500", 4.8, "Black sneaker"),
                        product("white", "Veja", "White Sneakers", "Footwear", "₹4,500", 4.2, "White sneaker")));

        assertEquals("white", results.getFirst().product().id());
    }

    @Test
    void currentQueryOverridesConflictingPersistedPreference() {
        UserPreference preference = preference(Set.of("Home"), Set.of("Luxury"), "Ferm Living");
        ShoppingIntent intent = intent("sneakers", "Footwear", "Sneakers");
        List<Recommendation> results = service.rank(request("sneakers", intent, preference),
                List.of(product("home", "Ferm Living", "Ripple Glass Set", "Home", "₹4,000", 4.9, "Glass set"),
                        product("shoe", "Veja", "Campo Sneakers", "Footwear", "₹4,000", 4.0, "Sneakers")));

        assertEquals("shoe", results.getFirst().product().id());
        assertTrue(results.getFirst().reasons().stream()
                .noneMatch(reason -> reason.type() == RecommendationReason.Type.PREFERENCE_CATEGORY));
    }

    @Test
    void favoriteBrandProvidesSmallerSecondaryBoost() {
        UserPreference preference = preference(Set.of(), Set.of(), "Aesop");
        List<Recommendation> results = service.rank(request("hand wash", null, preference),
                List.of(product("other", "Other", "Hand Wash", "Beauty", "₹1,000", 4.7, "Hand wash"),
                        product("aesop", "Aesop", "Hand Wash", "Beauty", "₹1,000", 4.7, "Hand wash")));

        assertEquals("aesop", results.getFirst().product().id());
    }

    @Test
    void higherRatingBreaksOtherwiseEquivalentMatch() {
        ShoppingIntent intent = intent("sneakers", "Footwear", "Sneakers");
        List<Recommendation> results = service.rank(request("sneakers", intent, null),
                List.of(product("low", "Veja", "Sneakers", "Footwear", "₹4,500", 4.1, "Sneakers"),
                        product("high", "Veja", "Sneakers", "Footwear", "₹4,500", 4.8, "Sneakers")));

        assertEquals("high", results.getFirst().product().id());
    }

    @Test
    void drySkinSignalsImproveRelevantProductRanking() {
        ShoppingIntent intent = new ShoppingIntent("moisturizer dry skin", "Beauty", "moisturizer",
                null, 1000D, List.of(), List.of(), List.of("dry skin"), List.of(),
                List.of("moisturizer", "dry skin"));
        List<Recommendation> results = service.rank(request("moisturizer dry skin", intent, null),
                List.of(product("generic", "Brand", "Daily Moisturizer", "Beauty", "₹700", 4.2,
                                "Light face moisturizer"),
                        product("hydrating", "Brand", "Hydrating Ceramide Moisturizer", "Beauty", "₹800", 4.1,
                                "Barrier support moisturizer")));

        assertEquals("hydrating", results.getFirst().product().id());
        assertTrue(results.getFirst().reasons().stream()
                .anyMatch(reason -> reason.type() == RecommendationReason.Type.USE_CASE));
    }

    @Test
    void reputableBrandSignalUsesProviderRatingAndBrandData() {
        ShoppingIntent intent = new ShoppingIntent("best moisturizer good brands", "Beauty", "moisturizer",
                null, 1000D, List.of(), List.of(), List.of(), List.of("best"),
                List.of("moisturizer"), null, "reputable-brands");
        List<Recommendation> results = service.rank(request("best moisturizer good brands", intent, null),
                List.of(product("unbranded", "", "Moisturizer", "Beauty", "₹700", 4.8, "Moisturizer"),
                        product("branded", "CeraVe", "Moisturizer", "Beauty", "₹800", 4.8, "Moisturizer")));

        assertEquals("branded", results.getFirst().product().id());
        assertTrue(results.getFirst().reasons().stream()
                .anyMatch(reason -> reason.type() == RecommendationReason.Type.QUALITY));
    }

    @Test
    void missingRatingDoesNotCrashRanking() {
        List<Recommendation> results = service.rank(request("shoes", null, null),
                List.of(product("missing-rating", "Veja", "Shoes", "Footwear", "₹4,500", Double.NaN, "Shoes")));

        assertEquals(1, results.size());
    }

    @Test
    void missingBrandDoesNotCrashRanking() {
        List<Recommendation> results = service.rank(request("shoes", null, null),
                List.of(product("missing-brand", null, "Shoes", "Footwear", "₹4,500", 4.0, "Shoes")));

        assertEquals(1, results.size());
    }

    @Test
    void missingDescriptionDoesNotCrashRanking() {
        List<Recommendation> results = service.rank(request("shoes", null, null),
                List.of(product("missing-description", "Veja", "Shoes", "Footwear", "₹4,500", 4.0, null)));

        assertEquals(1, results.size());
    }

    @Test
    void explanationsOnlyMentionSupportedReasons() {
        ShoppingIntent intent = new ShoppingIntent("white sneakers under ₹5000", "Footwear", "Sneakers",
                null, 5000D, List.of(), List.of("white"), List.of(), List.of(), List.of("white", "sneakers"));
        Recommendation result = service.rank(request("white sneakers under ₹5000", intent, null),
                        List.of(product("white", "Veja", "White Sneakers", "Footwear", "₹4,500", 4.8,
                                "White sneakers"))).getFirst();

        assertTrue(result.reasons().stream().anyMatch(reason -> reason.type() == RecommendationReason.Type.BUDGET));
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.type() == RecommendationReason.Type.COLOR_MATCH));
        assertTrue(result.reasons().stream().allMatch(reason -> !reason.text().contains("perfect")));
    }

    @Test
    void explanationsDoNotInventUnsupportedAttributes() {
        ShoppingIntent intent = new ShoppingIntent("comfortable sneakers", "Footwear", "Sneakers", null, null,
                List.of(), List.of(), List.of(), List.of("comfort"), List.of("sneakers", "comfortable"));
        Recommendation result = service.rank(request("comfortable sneakers", intent, null),
                        List.of(product("plain", "Veja", "Sneakers", "Footwear", "₹4,500", 4.5,
                                "White canvas sneaker"))).getFirst();

        assertTrue(result.reasons().stream().noneMatch(reason -> reason.text().toLowerCase().contains("comfort")));
        assertTrue(result.reasons().stream().noneMatch(reason -> reason.text().toLowerCase().contains("durable")));
    }

    @Test
    void identicalInputProducesDeterministicOrdering() {
        List<Product> products = List.of(
                product("b", "Brand", "Sneaker", "Footwear", "₹4,500", 4.5, "Sneaker"),
                product("a", "Brand", "Sneaker", "Footwear", "₹4,500", 4.5, "Sneaker"));
        SearchRequest request = request("sneakers", intent("sneakers", "Footwear", "Sneakers"), null);

        assertEquals(service.rank(request, products).stream().map(item -> item.product().id()).toList(),
                service.rank(request, products).stream().map(item -> item.product().id()).toList());
    }

    @Test
    void explicitPriceSortOverridesRecommendationOrder() {
        ShoppingIntent intent = intent("sneakers", "Footwear", "Sneakers");
        SearchRequest request = new SearchRequest("sneakers", null, 1, null, SearchFilters.none(),
                SearchSortMode.PRICE_LOW_TO_HIGH, intent);
        List<Recommendation> results = service.rank(request,
                List.of(product("strong-expensive", "Veja", "Sneakers", "Footwear", "₹8,000", 4.9, "Sneakers"),
                        product("weak-cheap", "Other", "Shoe", "Footwear", "₹1,000", 3.0, "Shoe")));

        assertEquals("weak-cheap", results.getFirst().product().id());
    }

    @Test
    void explicitRatingSortOverridesRecommendationOrder() {
        ShoppingIntent intent = intent("sneakers", "Footwear", "Sneakers");
        SearchRequest request = new SearchRequest("sneakers", null, 1, null, SearchFilters.none(),
                SearchSortMode.RATING, intent);
        List<Recommendation> results = service.rank(request,
                List.of(product("match-low-rating", "Veja", "Sneakers", "Footwear", "₹4,500", 3.5, "Sneakers"),
                        product("other-high-rating", "Other", "Shoe", "Footwear", "₹4,500", 4.9, "Shoe")));

        assertEquals("other-high-rating", results.getFirst().product().id());
    }

    @Test
    void mockProviderProductsCanBeRanked() {
        SearchService searchService = new SearchService(new MockProductSearchProvider(),
                request -> intent("footwear", "Footwear", "Sneakers"), null);

        var result = searchService.search(new SearchRequest("footwear", null, 1, null,
                SearchFilters.none(), SearchSortMode.RECOMMENDED));

        assertFalse(result.products().isEmpty());
        assertFalse(result.recommendations().isEmpty());
        assertEquals(result.products().getFirst().id(), result.recommendations().getFirst().product().id());
    }

    @Test
    void emptyProductListIsHandledCleanly() {
        assertTrue(service.rank(request("shoes", null, null), List.of()).isEmpty());
    }

    private SearchRequest request(String query, ShoppingIntent intent, UserPreference preference) {
        return new SearchRequest(query, null, 1, preference, SearchFilters.none(), SearchSortMode.RECOMMENDED, intent);
    }

    private ShoppingIntent intent(String query, String category, String productType) {
        return new ShoppingIntent(query, category, productType, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(productType.toLowerCase()));
    }

    private UserPreference preference(Set<String> categories, Set<String> priorities, String favoriteBrands) {
        return new UserPreference(1, categories, priorities, UserPreference.ShoppingStyle.NO_PREFERENCE,
                favoriteBrands, true, Instant.now());
    }

    private Product product(String id, String brand, String name, String category, String price,
                            double rating, String description) {
        return new Product(id, brand, name, category, price, null, null, rating, 10, "Store",
                IconType.SHOE, "artwork-olive", null, description, false);
    }
}
