package com.myshop.service;

import com.myshop.model.BehaviorProfile;
import com.myshop.model.HistoryEntry;
import com.myshop.model.Product;
import com.myshop.repository.HistoryRepository;

import java.util.List;
import java.util.Objects;

public final class HistoryService {
    private final HistoryRepository repository;

    public HistoryService(HistoryRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void recordSearch(long userId, String query) { repository.recordSearch(userId, query); }
    public List<HistoryEntry> recentSearches(long userId) { return repository.recentSearches(userId, 30); }
    public void clearSearches(long userId) { repository.clearSearches(userId); }
    public void recordViewedProduct(long userId, Product product) { repository.recordViewedProduct(userId, product); }
    public List<Product> recentlyViewed(long userId) { return repository.recentlyViewed(userId, 20); }
    public void clearViewed(long userId) { repository.clearViewed(userId); }
    public BehaviorProfile behaviorProfile(long userId) { return repository.behaviorProfile(userId); }
}
