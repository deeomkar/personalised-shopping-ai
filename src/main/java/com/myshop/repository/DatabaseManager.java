package com.myshop.repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {

    private static final String USERS_SCHEMA = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password_hash TEXT NOT NULL,
                created_at TEXT NOT NULL
            )
            """;

    private static final String USER_PREFERENCES_SCHEMA = """
            CREATE TABLE IF NOT EXISTS user_preferences (
                user_id INTEGER PRIMARY KEY,
                categories TEXT NOT NULL,
                priorities TEXT NOT NULL,
                shopping_style TEXT NOT NULL,
                favorite_brands TEXT NOT NULL DEFAULT '',
                onboarding_completed INTEGER NOT NULL DEFAULT 0,
                updated_at TEXT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;

    private static final String SAVED_PRODUCTS_SCHEMA = """
            CREATE TABLE IF NOT EXISTS saved_products (
                user_id INTEGER NOT NULL,
                product_id TEXT NOT NULL,
                brand TEXT, name TEXT NOT NULL, category TEXT, price TEXT, original_price TEXT, discount TEXT,
                rating REAL NOT NULL DEFAULT 0, review_count INTEGER NOT NULL DEFAULT 0, store TEXT,
                artwork TEXT, artwork_class TEXT, image_url TEXT, description TEXT,
                offer_product_id TEXT, offer_store_name TEXT, offer_price NUMERIC,
                offer_original_price NUMERIC, offer_currency TEXT, offer_url TEXT,
                offer_availability TEXT, offer_delivery TEXT, saved_at TEXT NOT NULL,
                PRIMARY KEY (user_id, product_id),
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;

    private static final String SEARCH_HISTORY_SCHEMA = """
            CREATE TABLE IF NOT EXISTS search_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL, query TEXT NOT NULL, searched_at TEXT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;

    private static final String VIEWED_PRODUCTS_SCHEMA = """
            CREATE TABLE IF NOT EXISTS viewed_products (
                user_id INTEGER NOT NULL,
                product_id TEXT NOT NULL,
                brand TEXT, name TEXT NOT NULL, category TEXT, price TEXT, original_price TEXT, discount TEXT,
                rating REAL NOT NULL DEFAULT 0, review_count INTEGER NOT NULL DEFAULT 0, store TEXT,
                artwork TEXT, artwork_class TEXT, image_url TEXT, description TEXT,
                offer_product_id TEXT, offer_store_name TEXT, offer_price NUMERIC,
                offer_original_price NUMERIC, offer_currency TEXT, offer_url TEXT,
                offer_availability TEXT, offer_delivery TEXT, viewed_at TEXT NOT NULL,
                PRIMARY KEY (user_id, product_id),
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;

    private final Path databasePath;

    public DatabaseManager(Path databasePath) {
        this.databasePath = databasePath.toAbsolutePath();
    }

    public Path databasePath() {
        return databasePath;
    }

    public void initialize() {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(USERS_SCHEMA);
            statement.executeUpdate(USER_PREFERENCES_SCHEMA);
            statement.executeUpdate(SAVED_PRODUCTS_SCHEMA);
            statement.executeUpdate(SEARCH_HISTORY_SCHEMA);
            statement.executeUpdate(VIEWED_PRODUCTS_SCHEMA);
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to initialize the MyShop database.", exception);
        }
    }

    public Connection openConnection() {
        try {
            Path parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
            }
            return connection;
        } catch (Exception exception) {
            throw new DatabaseException("Unable to open the MyShop database.", exception);
        }
    }
}
