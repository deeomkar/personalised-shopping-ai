package com.myshop.model;

import java.util.List;

/** Small, explainable signals derived from the current user's own activity. */
public record BehaviorProfile(List<String> categories, List<String> brands, List<String> searchTerms) {
    public BehaviorProfile {
        categories = List.copyOf(categories == null ? List.of() : categories);
        brands = List.copyOf(brands == null ? List.of() : brands);
        searchTerms = List.copyOf(searchTerms == null ? List.of() : searchTerms);
    }

    public static BehaviorProfile empty() {
        return new BehaviorProfile(List.of(), List.of(), List.of());
    }
}
