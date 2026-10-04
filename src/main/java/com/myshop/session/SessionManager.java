package com.myshop.session;

import com.myshop.model.User;

import java.util.Optional;

public final class SessionManager {

    private User currentUser;

    public void authenticate(User user) {
        currentUser = user;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public void clear() {
        currentUser = null;
    }
}
