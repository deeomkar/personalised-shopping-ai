package com.myshop.service;

import com.myshop.model.Product;

import java.util.ArrayList;
import java.util.List;

/** Session-scoped compare selection. It deliberately does not persist products. */
public final class CompareService {
    public static final int MAX_PRODUCTS = 3;
    private final List<Product> selected = new ArrayList<>();

    public boolean add(Product product) {
        if (product == null || selected.stream().anyMatch(item -> item.id().equals(product.id()))
                || selected.size() >= MAX_PRODUCTS) return false;
        selected.add(product);
        return true;
    }

    public boolean remove(String productId) {
        return selected.removeIf(product -> product.id().equals(productId));
    }

    public boolean toggle(Product product) {
        if (contains(product.id())) {
            remove(product.id());
            return false;
        }
        return add(product);
    }

    public boolean contains(String productId) {
        return selected.stream().anyMatch(product -> product.id().equals(productId));
    }

    public void clear() { selected.clear(); }

    public List<Product> products() { return List.copyOf(selected); }
}
