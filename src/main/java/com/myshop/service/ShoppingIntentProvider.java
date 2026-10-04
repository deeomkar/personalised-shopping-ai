package com.myshop.service;

import com.myshop.model.SearchRequest;
import com.myshop.model.ShoppingIntent;

/** Provider-neutral seam for understanding natural-language shopping intent. */
@FunctionalInterface
public interface ShoppingIntentProvider {
    ShoppingIntent understand(SearchRequest request);
}
