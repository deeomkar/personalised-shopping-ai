package com.myshop.service;

import com.myshop.model.Product;
import com.myshop.repository.SavedProductRepository;

import java.util.List;
import java.util.Objects;

public final class SavedProductService {
    private final SavedProductRepository repository;

    public SavedProductService(SavedProductRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void save(long userId, Product product) {
        repository.save(userId, product);
    }

    public void unsave(long userId, String productId) {
        repository.delete(userId, productId);
    }

    public boolean isSaved(long userId, String productId) {
        return repository.exists(userId, productId);
    }

    public boolean toggle(long userId, Product product) {
        if (isSaved(userId, product.id())) {
            unsave(userId, product.id());
            return false;
        }
        save(userId, product);
        return true;
    }

    public List<Product> list(long userId) {
        return repository.findAll(userId);
    }
}
