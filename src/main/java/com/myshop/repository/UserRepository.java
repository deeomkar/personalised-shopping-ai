package com.myshop.repository;

import com.myshop.model.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByEmail(String email);

    Optional<User> findById(long id);

    boolean existsByEmail(String email);

    User save(User user);
}
