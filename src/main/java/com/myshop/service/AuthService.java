package com.myshop.service;

import com.myshop.model.User;
import com.myshop.repository.DatabaseException;
import com.myshop.repository.UserRepository;
import com.myshop.security.PasswordHasher;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

public final class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public User register(String name, String email, String password) {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedEmail = normalizeEmail(email);
        validateRegistration(normalizedName, normalizedEmail, password);

        try {
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new AuthException(AuthException.Code.DUPLICATE_EMAIL,
                        "An account with this email already exists.");
            }

            User pendingUser = new User(
                    0L,
                    normalizedName,
                    normalizedEmail,
                    passwordHasher.hash(password),
                    Instant.now()
            );
            return userRepository.save(pendingUser);
        } catch (AuthException exception) {
            throw exception;
        } catch (DatabaseException exception) {
            throw new AuthException(AuthException.Code.PERSISTENCE,
                    "We couldn't create your account. Please try again.", exception);
        }
    }

    public User login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail) || password == null || password.isEmpty()) {
            throw invalidCredentials();
        }

        try {
            return userRepository.findByEmail(normalizedEmail)
                    .filter(user -> passwordHasher.matches(password, user.passwordHash()))
                    .orElseThrow(this::invalidCredentials);
        } catch (AuthException exception) {
            throw exception;
        } catch (DatabaseException exception) {
            throw new AuthException(AuthException.Code.PERSISTENCE,
                    "We couldn't sign you in. Please try again.", exception);
        }
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    private void validateRegistration(String name, String email, String password) {
        if (name.isBlank()) {
            throw new AuthException(AuthException.Code.VALIDATION, "Enter your name.");
        }
        if (!isValidEmail(email)) {
            throw new AuthException(AuthException.Code.VALIDATION, "Enter a valid email address.");
        }
        if (!isValidPassword(password)) {
            throw new AuthException(AuthException.Code.VALIDATION, "Password must be at least 8 characters.");
        }
    }

    private AuthException invalidCredentials() {
        return new AuthException(AuthException.Code.INVALID_CREDENTIALS,
                "Email or password is incorrect.");
    }
}
