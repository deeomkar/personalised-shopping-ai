package com.myshop.service;

import com.myshop.model.Product;
import com.myshop.model.SearchRequest;

import java.util.List;

/**
 * Boundary for a selected live product-data service.
 *
 * <p>Implementations belong in provider-specific adapters. This interface
 * keeps HTTP, authentication, and response mapping out of the application
 * search pipeline.</p>
 */
@FunctionalInterface
public interface LiveProductSearchAdapter {

    List<Product> search(SearchRequest request);
}
