package com.myshop.repository;

import com.myshop.model.BehaviorProfile;
import com.myshop.model.HistoryEntry;
import com.myshop.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SqliteHistoryRepository implements HistoryRepository {
    private final DatabaseManager database;

    public SqliteHistoryRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public void recordSearch(long userId, String query) {
        if (query == null || query.isBlank()) return;
        try (Connection connection = database.openConnection()) {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM search_history WHERE user_id = ? AND query = ?")) {
                delete.setLong(1, userId); delete.setString(2, query.trim()); delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO search_history(user_id, query, searched_at) VALUES (?, ?, ?)")) {
                insert.setLong(1, userId); insert.setString(2, query.trim()); insert.setString(3, Instant.now().toString()); insert.executeUpdate();
            }
            trimSearches(connection, userId);
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to record search history.", exception);
        }
    }

    @Override
    public List<HistoryEntry> recentSearches(long userId, int limit) {
        String sql = "SELECT query, searched_at FROM search_history WHERE user_id = ? ORDER BY searched_at DESC LIMIT ?";
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId); statement.setInt(2, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<HistoryEntry> entries = new ArrayList<>();
                while (resultSet.next()) entries.add(new HistoryEntry(resultSet.getString("query"), Instant.parse(resultSet.getString("searched_at"))));
                return List.copyOf(entries);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load search history.", exception);
        }
    }

    @Override
    public void clearSearches(long userId) {
        executeUserDelete("search_history", userId);
    }

    @Override
    public void recordViewedProduct(long userId, Product product) {
        String sql = """
                INSERT INTO viewed_products (
                    user_id, product_id, brand, name, category, price, original_price, discount,
                    rating, review_count, store, artwork, artwork_class, image_url, description,
                    offer_product_id, offer_store_name, offer_price, offer_original_price, offer_currency,
                    offer_url, offer_availability, offer_delivery, viewed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(user_id, product_id) DO UPDATE SET
                    brand=excluded.brand, name=excluded.name, category=excluded.category, price=excluded.price,
                    original_price=excluded.original_price, discount=excluded.discount, rating=excluded.rating,
                    review_count=excluded.review_count, store=excluded.store, artwork=excluded.artwork,
                    artwork_class=excluded.artwork_class, image_url=excluded.image_url, description=excluded.description,
                    offer_product_id=excluded.offer_product_id, offer_store_name=excluded.offer_store_name,
                    offer_price=excluded.offer_price, offer_original_price=excluded.offer_original_price,
                    offer_currency=excluded.offer_currency, offer_url=excluded.offer_url,
                    offer_availability=excluded.offer_availability, offer_delivery=excluded.offer_delivery,
                    viewed_at=excluded.viewed_at
                """;
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            ProductPersistenceMapper.bindProduct(statement, 2, product);
            statement.setString(24, Instant.now().toString());
            statement.executeUpdate();
            trimViewed(connection, userId);
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to record viewed product.", exception);
        }
    }

    @Override
    public List<Product> recentlyViewed(long userId, int limit) {
        String sql = "SELECT * FROM viewed_products WHERE user_id = ? ORDER BY viewed_at DESC LIMIT ?";
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId); statement.setInt(2, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Product> products = new ArrayList<>();
                while (resultSet.next()) products.add(ProductPersistenceMapper.readProduct(resultSet));
                return List.copyOf(products);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load recently viewed products.", exception);
        }
    }

    @Override
    public void clearViewed(long userId) {
        executeUserDelete("viewed_products", userId);
    }

    @Override
    public BehaviorProfile behaviorProfile(long userId) {
        String sql = "SELECT category, brand FROM viewed_products WHERE user_id = ? ORDER BY viewed_at DESC LIMIT 20";
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            List<String> categories = new ArrayList<>();
            List<String> brands = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    addIfPresent(categories, resultSet.getString("category"));
                    addIfPresent(brands, resultSet.getString("brand"));
                }
            }
            return new BehaviorProfile(categories.stream().distinct().toList(), brands.stream().distinct().toList(),
                    recentSearches(userId, 20).stream().map(HistoryEntry::query).toList());
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load behavior profile.", exception);
        }
    }

    private void trimSearches(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM search_history WHERE user_id = ? AND id NOT IN "
                        + "(SELECT id FROM search_history WHERE user_id = ? ORDER BY searched_at DESC LIMIT 30)")) {
            statement.setLong(1, userId); statement.setLong(2, userId); statement.executeUpdate();
        }
    }

    private void trimViewed(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM viewed_products WHERE user_id = ? AND product_id NOT IN "
                        + "(SELECT product_id FROM viewed_products WHERE user_id = ? ORDER BY viewed_at DESC LIMIT 20)")) {
            statement.setLong(1, userId); statement.setLong(2, userId); statement.executeUpdate();
        }
    }

    private void executeUserDelete(String table, long userId) {
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM " + table + " WHERE user_id = ?")) {
            statement.setLong(1, userId); statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to clear history.", exception);
        }
    }

    private void addIfPresent(List<String> values, String value) {
        if (value != null && !value.isBlank()) values.add(value);
    }
}
