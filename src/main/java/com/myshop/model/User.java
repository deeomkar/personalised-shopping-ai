package com.myshop.model;

import java.time.Instant;
import java.util.Objects;

public record User(
        long id,
        String name,
        String email,
        String passwordHash,
        Instant createdAt
) {
    public User {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
