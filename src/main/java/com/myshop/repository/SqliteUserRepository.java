package com.myshop.repository;

import com.myshop.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;

public final class SqliteUserRepository implements UserRepository {

    private final DatabaseManager database;

    public SqliteUserRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, name, email, password_hash, created_at FROM users WHERE email = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find the user by email.", exception);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT id, name, email, password_hash, created_at FROM users WHERE id = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to find the user by id.", exception);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to check whether the email exists.", exception);
        }
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (name, email, password_hash, created_at) VALUES (?, ?, ?, ?)";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.name());
            statement.setString(2, user.email());
            statement.setString(3, user.passwordHash());
            statement.setString(4, user.createdAt().toString());
            statement.executeUpdate();

            long id;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                id = keys.next() ? keys.getLong(1) : 0L;
            }
            if (id == 0L) {
                try (Statement fallback = connection.createStatement();
                     ResultSet resultSet = fallback.executeQuery("SELECT last_insert_rowid()")) {
                    if (!resultSet.next()) {
                        throw new DatabaseException("SQLite did not return the created user id.", null);
                    }
                    id = resultSet.getLong(1);
                }
            }
            return new User(id, user.name(), user.email(), user.passwordHash(), user.createdAt());
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to save the user.", exception);
        }
    }

    private User map(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getString("password_hash"),
                Instant.parse(resultSet.getString("created_at"))
        );
    }
}
