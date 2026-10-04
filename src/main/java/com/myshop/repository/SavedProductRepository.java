package com.myshop.repository;

import com.myshop.model.Product;

import java.util.List;

public interface SavedProductRepository {
    void save(long userId, Product product);
    void delete(long userId, String productId);
    boolean exists(long userId, String productId);
    List<Product> findAll(long userId);
}
