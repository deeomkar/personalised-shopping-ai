package com.myshop.repository;

import com.myshop.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class SqliteSavedProductRepository implements SavedProductRepository {
    private final DatabaseManager database;

    public SqliteSavedProductRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public void save(long userId, Product product) {
        String sql = """
                INSERT INTO saved_products (
                    user_id, product_id, brand, name, category, price, original_price, discount,
                    rating, review_count, store, artwork, artwork_class, image_url, description,
                    offer_product_id, offer_store_name, offer_price, offer_original_price, offer_currency,
                    offer_url, offer_availability, offer_delivery, saved_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(user_id, product_id) DO UPDATE SET
                    brand=excluded.brand, name=excluded.name, category=excluded.category,
                    price=excluded.price, original_price=excluded.original_price, discount=excluded.discount,
                    rating=excluded.rating, review_count=excluded.review_count, store=excluded.store,
                    artwork=excluded.artwork, artwork_class=excluded.artwork_class, image_url=excluded.image_url,
                    description=excluded.description, offer_product_id=excluded.offer_product_id,
                    offer_store_name=excluded.offer_store_name, offer_price=excluded.offer_price,
                    offer_original_price=excluded.offer_original_price, offer_currency=excluded.offer_currency,
                    offer_url=excluded.offer_url, offer_availability=excluded.offer_availability,
                    offer_delivery=excluded.offer_delivery, saved_at=excluded.saved_at
                """;
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            ProductPersistenceMapper.bindProduct(statement, 2, product);
            statement.setString(24, java.time.Instant.now().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to save the product.", exception);
        }
    }

    @Override
    public void delete(long userId, String productId) {
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM saved_products WHERE user_id = ? AND product_id = ?")) {
            statement.setLong(1, userId);
            statement.setString(2, productId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to remove the saved product.", exception);
        }
    }

    @Override
    public boolean exists(long userId, String productId) {
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM saved_products WHERE user_id = ? AND product_id = ?")) {
            statement.setLong(1, userId);
            statement.setString(2, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to check the saved product.", exception);
        }
    }

    @Override
    public List<Product> findAll(long userId) {
        String sql = "SELECT * FROM saved_products WHERE user_id = ? ORDER BY saved_at DESC";
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Product> products = new ArrayList<>();
                while (resultSet.next()) {
                    products.add(ProductPersistenceMapper.readProduct(resultSet));
                }
                return List.copyOf(products);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load saved products.", exception);
        }
    }
}
