package com.myshop.service;

import com.myshop.model.User;
import com.myshop.model.UserPreference;
import com.myshop.repository.DatabaseManager;
import com.myshop.repository.SqliteUserPreferenceRepository;
import com.myshop.repository.SqliteUserRepository;
import com.myshop.security.BCryptPasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferenceServiceTest {

    @TempDir
    Path temporaryDirectory;

    private PreferenceService preferenceService;
    private User user;

    @BeforeEach
    void setUp() {
        DatabaseManager database = new DatabaseManager(temporaryDirectory.resolve("test-myshop.db"));
        database.initialize();
        AuthService authService = new AuthService(
                new SqliteUserRepository(database),
                new BCryptPasswordHasher()
        );
        preferenceService = new PreferenceService(new SqliteUserPreferenceRepository(database));
        user = authService.register("Asha Rao", "asha@example.com", "correct-horse");
    }

    @Test
    void savingPreferencesPersistsTheCompletedSelection() {
        UserPreference saved = preferenceService.completeOnboarding(
                user.id(),
                Set.of("Fashion", "Footwear"),
                Set.of("Price", "Quality"),
                UserPreference.ShoppingStyle.BEST_VALUE,
                "Nike, Adidas"
        );

        assertTrue(saved.onboardingCompleted());
        assertEquals("Nike, Adidas", saved.favoriteBrands());
    }

    @Test
    void preferencesCanBeLoadedByUser() {
        preferenceService.completeOnboarding(
                user.id(), Set.of("Electronics"), Set.of("Ratings"),
                UserPreference.ShoppingStyle.PREMIUM, "Sony"
        );

        UserPreference loaded = preferenceService.load(user.id()).orElseThrow();

        assertEquals(Set.of("Electronics"), loaded.selectedCategories());
        assertEquals(Set.of("Ratings"), loaded.shoppingPriorities());
        assertEquals(UserPreference.ShoppingStyle.PREMIUM, loaded.shoppingStyle());
    }

    @Test
    void newUserHasIncompleteOnboarding() {
        assertFalse(preferenceService.hasCompletedOnboarding(user.id()));
        assertTrue(preferenceService.load(user.id()).isEmpty());
    }

    @Test
    void completedOnboardingReturnsTrue() {
        preferenceService.completeOnboarding(
                user.id(), Set.of("Home"), Set.of("Comfort"),
                UserPreference.ShoppingStyle.NO_PREFERENCE, ""
        );

        assertTrue(preferenceService.hasCompletedOnboarding(user.id()));
    }

    @Test
    void existingPreferencesCanBeUpdated() {
        preferenceService.completeOnboarding(
                user.id(), Set.of("Beauty"), Set.of("Brand"),
                UserPreference.ShoppingStyle.PREMIUM, "Clinique"
        );

        preferenceService.completeOnboarding(
                user.id(), Set.of("Sports"), Set.of("Durability", "Comfort"),
                UserPreference.ShoppingStyle.BUDGET_CONSCIOUS, "Nike"
        );

        UserPreference updated = preferenceService.load(user.id()).orElseThrow();
        assertEquals(Set.of("Sports"), updated.selectedCategories());
        assertEquals(Set.of("Durability", "Comfort"), updated.shoppingPriorities());
        assertEquals(UserPreference.ShoppingStyle.BUDGET_CONSCIOUS, updated.shoppingStyle());
        assertEquals("Nike", updated.favoriteBrands());
    }

    @Test
    void categoryValidationRequiresKnownCategory() {
        PreferenceException exception = assertThrows(
                PreferenceException.class,
                () -> preferenceService.validateCategories(Set.of("Fashion", "Unknown"))
        );

        assertEquals(PreferenceException.Code.VALIDATION, exception.code());
    }

    @Test
    void priorityValidationAllowsAtMostFourKnownPriorities() {
        PreferenceException exception = assertThrows(
                PreferenceException.class,
                () -> preferenceService.validatePriorities(
                        Set.of("Price", "Quality", "Ratings", "Brand", "Style")
                )
        );

        assertEquals(PreferenceException.Code.VALIDATION, exception.code());
    }
}
