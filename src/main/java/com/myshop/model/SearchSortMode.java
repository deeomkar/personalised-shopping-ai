package com.myshop.model;

public enum SearchSortMode {
    RECOMMENDED("Recommended"),
    PRICE_LOW_TO_HIGH("Price: Low to High"),
    PRICE_HIGH_TO_LOW("Price: High to Low"),
    RATING("Rating");

    private final String label;

    SearchSortMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
