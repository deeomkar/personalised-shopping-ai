package com.myshop.repository;

import com.myshop.model.UserPreference;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class SqliteUserPreferenceRepository implements UserPreferenceRepository {

    private final DatabaseManager database;

    public SqliteUserPreferenceRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public Optional<UserPreference> findByUserId(long userId) {
        String sql = """
                SELECT user_id, categories, priorities, shopping_style, favorite_brands,
                       onboarding_completed, updated_at
                FROM user_preferences
                WHERE user_id = ?
                """;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load user preferences.", exception);
        }
    }

    @Override
    public UserPreference save(UserPreference preferences) {
        String sql = """
                INSERT INTO user_preferences (
                    user_id, categories, priorities, shopping_style,
                    favorite_brands, onboarding_completed, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(user_id) DO UPDATE SET
                    categories = excluded.categories,
                    priorities = excluded.priorities,
                    shopping_style = excluded.shopping_style,
                    favorite_brands = excluded.favorite_brands,
                    onboarding_completed = excluded.onboarding_completed,
                    updated_at = excluded.updated_at
                """;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, preferences.userId());
            statement.setString(2, serialize(preferences.selectedCategories()));
            statement.setString(3, serialize(preferences.shoppingPriorities()));
            statement.setString(4, preferences.shoppingStyle().name());
            statement.setString(5, preferences.favoriteBrands());
            statement.setBoolean(6, preferences.onboardingCompleted());
            statement.setString(7, preferences.updatedAt().toString());
            statement.executeUpdate();
            return preferences;
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to save user preferences.", exception);
        }
    }

    @Override
    public boolean hasCompletedOnboarding(long userId) {
        String sql = "SELECT onboarding_completed FROM user_preferences WHERE user_id = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean("onboarding_completed");
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to check onboarding status.", exception);
        }
    }

    private UserPreference map(ResultSet resultSet) throws SQLException {
        return new UserPreference(
                resultSet.getLong("user_id"),
                deserialize(resultSet.getString("categories")),
                deserialize(resultSet.getString("priorities")),
                UserPreference.ShoppingStyle.valueOf(resultSet.getString("shopping_style")),
                resultSet.getString("favorite_brands"),
                resultSet.getBoolean("onboarding_completed"),
                Instant.parse(resultSet.getString("updated_at"))
        );
    }

    private String serialize(Set<String> values) {
        return String.join(",", values);
    }

    private Set<String> deserialize(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.asList(value.split(",", -1)));
    }
}
