package com.myshop.controller;

import com.myshop.model.User;
import com.myshop.service.AuthService;

public final class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public User login(String email, String password) {
        return authService.login(email, password);
    }

    public User register(String name, String email, String password) {
        return authService.register(name, email, password);
    }
}
