package com.myshop.service;

import com.myshop.component.IconType;
import com.myshop.model.Product;
import com.myshop.model.User;
import com.myshop.repository.DatabaseManager;
import com.myshop.repository.SqliteHistoryRepository;
import com.myshop.repository.SqliteSavedProductRepository;
import com.myshop.repository.SqliteUserRepository;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceServicesTest {
    @Test
    void savedProductsPersistAndRemainIsolatedByUser() throws Exception {
        DatabaseManager database = new DatabaseManager(Files.createTempFile("myshop-saved", ".db")); database.initialize();
        User userA = user(database, "a@example.com"); User userB = user(database, "b@example.com");
        SavedProductService service = new SavedProductService(new SqliteSavedProductRepository(database));
        Product product = product("p1", "Aesop");
        service.save(userA.id(), product); service.save(userA.id(), product);
        assertTrue(service.isSaved(userA.id(), "p1")); assertEquals(1, service.list(userA.id()).size()); assertTrue(service.list(userB.id()).isEmpty());
        service.unsave(userA.id(), "p1"); assertFalse(service.isSaved(userA.id(), "p1"));
    }

    @Test
    void historyPersistsSearchesAndViewedProductsWithDedupeAndClear() throws Exception {
        DatabaseManager database = new DatabaseManager(Files.createTempFile("myshop-history", ".db")); database.initialize();
        User user = user(database, "history@example.com"); HistoryService service = new HistoryService(new SqliteHistoryRepository(database));
        service.recordSearch(user.id(), "white sneakers"); service.recordSearch(user.id(), "white sneakers");
        service.recordViewedProduct(user.id(), product("p1", "Veja")); service.recordViewedProduct(user.id(), product("p1", "Veja"));
        assertEquals(1, service.recentSearches(user.id()).size()); assertEquals(1, service.recentlyViewed(user.id()).size());
        service.clearSearches(user.id()); service.clearViewed(user.id());
        assertTrue(service.recentSearches(user.id()).isEmpty()); assertTrue(service.recentlyViewed(user.id()).isEmpty());
    }

    @Test
    void savedAndViewedProductsPreserveLinkOnlyExternalUrl() throws Exception {
        DatabaseManager database = new DatabaseManager(Files.createTempFile("myshop-links", ".db"));
        database.initialize();
        User user = user(database, "links@example.com");
        Product product = new Product(
                "link-product", "Brand", "Product", "Beauty", "₹999", null, null,
                4.5, 10, "Store", IconType.BAG, "artwork-sand", null, "Description", false,
                List.of(), "https://store.example/product"
        );

        new SavedProductService(new SqliteSavedProductRepository(database)).save(user.id(), product);
        HistoryService history = new HistoryService(new SqliteHistoryRepository(database));
        history.recordViewedProduct(user.id(), product);

        assertEquals("https://store.example/product",
                new SqliteSavedProductRepository(database).findAll(user.id()).getFirst().externalUrl());
        assertEquals("https://store.example/product",
                history.recentlyViewed(user.id()).getFirst().externalUrl());
    }

    private User user(DatabaseManager database, String email) {
        return new SqliteUserRepository(database).save(new User(0, email, email, "hash", Instant.now()));
    }

    private Product product(String id, String brand) {
        return new Product(id, brand, "Product", "Beauty", "₹1,000", null, null, 4.5, 10, brand,
                IconType.BAG, "artwork-sand", null, "Description", false);
    }
}
