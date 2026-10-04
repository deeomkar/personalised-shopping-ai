package com.myshop.service;

import com.myshop.model.User;
import com.myshop.repository.DatabaseManager;
import com.myshop.repository.SqliteUserRepository;
import com.myshop.security.BCryptPasswordHasher;
import com.myshop.session.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    @TempDir
    Path temporaryDirectory;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        DatabaseManager database = new DatabaseManager(temporaryDirectory.resolve("test-myshop.db"));
        database.initialize();
        authService = new AuthService(
                new SqliteUserRepository(database),
                new BCryptPasswordHasher()
        );
    }

    @Test
    void validRegistrationSucceeds() {
        User user = authService.register("Asha Rao", "ASHA@example.com", "correct-horse");

        assertTrue(user.id() > 0);
        assertEquals("Asha Rao", user.name());
        assertEquals("asha@example.com", user.email());
        assertNotNull(user.createdAt());
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() {
        authService.register("Asha Rao", "asha@example.com", "correct-horse");

        AuthException exception = assertThrows(
                AuthException.class,
                () -> authService.register("Another User", " ASHA@EXAMPLE.COM ", "another-pass")
        );

        assertEquals(AuthException.Code.DUPLICATE_EMAIL, exception.code());
    }

    @Test
    void invalidEmailIsRejected() {
        AuthException exception = assertThrows(
                AuthException.class,
                () -> authService.register("Asha Rao", "not-an-email", "correct-horse")
        );

        assertEquals(AuthException.Code.VALIDATION, exception.code());
    }

    @Test
    void shortPasswordIsRejected() {
        AuthException exception = assertThrows(
                AuthException.class,
                () -> authService.register("Asha Rao", "asha@example.com", "short")
        );

        assertEquals(AuthException.Code.VALIDATION, exception.code());
    }

    @Test
    void correctLoginSucceeds() {
        User registered = authService.register("Asha Rao", "asha@example.com", "correct-horse");

        User loggedIn = authService.login(" ASHA@EXAMPLE.COM ", "correct-horse");

        assertEquals(registered.id(), loggedIn.id());
        assertEquals(registered.email(), loggedIn.email());
    }

    @Test
    void incorrectPasswordUsesGenericCredentialError() {
        authService.register("Asha Rao", "asha@example.com", "correct-horse");

        AuthException exception = assertThrows(
                AuthException.class,
                () -> authService.login("asha@example.com", "wrong-password")
        );

        assertEquals(AuthException.Code.INVALID_CREDENTIALS, exception.code());
        assertEquals("Email or password is incorrect.", exception.getMessage());
    }

    @Test
    void storedPasswordIsHashed() {
        User user = authService.register("Asha Rao", "asha@example.com", "correct-horse");

        assertNotEquals("correct-horse", user.passwordHash());
        assertTrue(user.passwordHash().startsWith("$2"));
    }

    @Test
    void sessionIsPopulatedAfterAuthentication() {
        User user = authService.register("Asha Rao", "asha@example.com", "correct-horse");
        SessionManager session = new SessionManager();

        assertFalse(session.isAuthenticated());
        session.authenticate(user);
        assertTrue(session.isAuthenticated());
        assertEquals(user.id(), session.currentUser().orElseThrow().id());
    }

    @Test
    void logoutClearsSession() {
        User user = authService.register("Asha Rao", "asha@example.com", "correct-horse");
        SessionManager session = new SessionManager();
        session.authenticate(user);

        session.clear();

        assertFalse(session.isAuthenticated());
        assertTrue(session.currentUser().isEmpty());
    }
}
