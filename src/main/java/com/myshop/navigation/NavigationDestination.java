package com.myshop.navigation;

import com.myshop.component.IconType;

public enum NavigationDestination {
    DISCOVER("Discover", IconType.DISCOVER, true),
    SAVED("Saved", IconType.SAVED, true),
    COMPARE("Compare", IconType.COMPARE, true),
    SEARCH_RESULTS("Search results", IconType.SEARCH, false),
    PRODUCT_DETAILS("Product details", IconType.BAG, false),
    PROFILE("Profile & settings", IconType.USER, false);

    private final String label;
    private final IconType icon;
    private final boolean primary;

    NavigationDestination(String label, IconType icon, boolean primary) {
        this.label = label;
        this.icon = icon;
        this.primary = primary;
    }

    public String label() {
        return label;
    }

    public IconType icon() {
        return icon;
    }

    public boolean primary() {
        return primary;
    }
}
