package com.myshop.service;

import com.myshop.model.SearchRequest;
import com.myshop.model.SearchResult;
import com.myshop.model.SearchFilters;
import com.myshop.model.Recommendation;
import com.myshop.model.ShoppingIntent;
import com.myshop.model.UserPreference;

import java.util.List;
import java.util.Objects;

public final class SearchService {

    private final ProductSearchProvider provider;
    private final ShoppingIntentProvider intentProvider;
    private final ShoppingIntentProvider fallbackIntentProvider;
    private final PreferenceService preferenceService;
    private final RecommendationService recommendationService;
    private final HistoryService historyService;

    public SearchService(ProductSearchProvider provider) {
        this(provider, new FallbackShoppingIntentProvider(), null, new RecommendationService(), null);
    }

    public SearchService(ProductSearchProvider provider, PreferenceService preferenceService) {
        this(provider, new ShoppingIntentProviderChain(), preferenceService, new RecommendationService(), null);
    }

    public SearchService(ProductSearchProvider provider, ShoppingIntentProvider intentProvider,
                         PreferenceService preferenceService) {
        this(provider, intentProvider, preferenceService, new RecommendationService(), null);
    }

    public SearchService(ProductSearchProvider provider, ShoppingIntentProvider intentProvider,
                         PreferenceService preferenceService, RecommendationService recommendationService) {
        this(provider, intentProvider, preferenceService, recommendationService, null);
    }

    public SearchService(ProductSearchProvider provider, ShoppingIntentProvider intentProvider,
                         PreferenceService preferenceService, RecommendationService recommendationService,
                         HistoryService historyService) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.intentProvider = Objects.requireNonNull(intentProvider, "intentProvider");
        this.fallbackIntentProvider = new FallbackShoppingIntentProvider();
        this.preferenceService = preferenceService;
        this.recommendationService = Objects.requireNonNull(recommendationService, "recommendationService");
        this.historyService = historyService;
    }

    public SearchResult search(SearchRequest request) {
        Objects.requireNonNull(request, "request");
        if (!request.hasSearchIntent()) {
            throw new IllegalArgumentException("A search query or category is required");
        }

        SearchRequest enrichedRequest = enrichPreferences(request);
        ShoppingIntent intent = enrichedRequest.shoppingIntent() == null
                ? understandWithFallback(enrichedRequest) : enrichedRequest.shoppingIntent();
        SearchRequest normalizedRequest = applyIntent(enrichedRequest, intent);
        try {
            List<com.myshop.model.Product> products = provider.search(normalizedRequest);
            if (historyService != null && request.shoppingIntent() == null && !request.rawQuery().isBlank()) {
                historyService.recordSearch(request.userId(), request.rawQuery());
            }
            var behavior = historyService == null || request.userId() <= 0
                    ? com.myshop.model.BehaviorProfile.empty() : historyService.behaviorProfile(request.userId());
            List<Recommendation> recommendations = recommendationService.rank(normalizedRequest, products, behavior);
            return SearchResult.successWithRecommendations(normalizedRequest, recommendations);
        } catch (RuntimeException exception) {
            return SearchResult.error(normalizedRequest, "We couldn't load products right now.");
        }
    }

    private ShoppingIntent understandWithFallback(SearchRequest request) {
        try {
            ShoppingIntent intent = intentProvider.understand(request);
            return intent == null ? fallbackIntentProvider.understand(request) : intent;
        } catch (RuntimeException exception) {
            return fallbackIntentProvider.understand(request);
        }
    }

    private SearchRequest applyIntent(SearchRequest request, ShoppingIntent intent) {
        SearchFilters filters = request.filters();
        String category = request.category();
        if (category.isBlank() && filters.category() == null && intent.category() != null) {
            category = intent.category();
        }
        Double minimum = filters.minimumPrice() == null ? intent.minBudget() : filters.minimumPrice();
        Double maximum = filters.maximumPrice() == null ? intent.maxBudget() : filters.maximumPrice();
        SearchFilters enrichedFilters = new SearchFilters(
                filters.category(), filters.brand(), minimum, maximum, filters.minimumRating()
        );
        return new SearchRequest(request.rawQuery(), category, request.userId(), request.userPreference(),
                enrichedFilters, request.sortMode(), intent);
    }

    private SearchRequest enrichPreferences(SearchRequest request) {
        if (request.userPreference() != null || preferenceService == null || request.userId() <= 0) {
            return request;
        }
        UserPreference preference = preferenceService.load(request.userId()).orElse(null);
        return new SearchRequest(
                request.rawQuery(), request.category(), request.userId(), preference,
                request.filters(), request.sortMode(), request.shoppingIntent()
        );
    }
}
