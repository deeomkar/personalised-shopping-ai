package com.myshop.repository;

import com.myshop.model.BehaviorProfile;
import com.myshop.model.HistoryEntry;
import com.myshop.model.Product;

import java.util.List;

public interface HistoryRepository {
    void recordSearch(long userId, String query);
    List<HistoryEntry> recentSearches(long userId, int limit);
    void clearSearches(long userId);
    void recordViewedProduct(long userId, Product product);
    List<Product> recentlyViewed(long userId, int limit);
    void clearViewed(long userId);
    BehaviorProfile behaviorProfile(long userId);
}
