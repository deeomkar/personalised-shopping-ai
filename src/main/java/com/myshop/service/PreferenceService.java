package com.myshop.service;

import com.myshop.model.UserPreference;
import com.myshop.repository.DatabaseException;
import com.myshop.repository.UserPreferenceRepository;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class PreferenceService {

    public static final List<String> CATEGORY_OPTIONS = List.of(
            "Fashion", "Footwear", "Electronics", "Beauty", "Accessories", "Home", "Sports"
    );
    public static final List<String> PRIORITY_OPTIONS = List.of(
            "Price", "Quality", "Ratings", "Brand", "Style", "Durability", "Comfort"
    );
    public static final int MAX_PRIORITIES = 4;

    private final UserPreferenceRepository preferenceRepository;

    public PreferenceService(UserPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    public Optional<UserPreference> load(long userId) {
        try {
            return preferenceRepository.findByUserId(userId);
        } catch (DatabaseException exception) {
            throw persistenceError("We couldn't load your shopping preferences.", exception);
        }
    }

    public boolean hasCompletedOnboarding(long userId) {
        try {
            return preferenceRepository.hasCompletedOnboarding(userId);
        } catch (DatabaseException exception) {
            throw persistenceError("We couldn't check your onboarding status.", exception);
        }
    }

    public UserPreference completeOnboarding(
            long userId,
            Set<String> selectedCategories,
            Set<String> shoppingPriorities,
            UserPreference.ShoppingStyle shoppingStyle,
            String favoriteBrands
    ) {
        UserPreference preferences = new UserPreference(
                userId,
                selectedCategories,
                shoppingPriorities,
                shoppingStyle,
                favoriteBrands,
                true,
                Instant.now()
        );
        return save(preferences);
    }

    public UserPreference save(UserPreference preferences) {
        validate(preferences);
        try {
            return preferenceRepository.save(preferences);
        } catch (DatabaseException exception) {
            throw persistenceError("We couldn't save your shopping preferences.", exception);
        }
    }

    public void validateCategories(Set<String> selectedCategories) {
        if (selectedCategories == null || selectedCategories.isEmpty()) {
            throw validationError("Choose at least one category.");
        }
        if (!CATEGORY_OPTIONS.containsAll(selectedCategories)
                || selectedCategories.size() != new LinkedHashSet<>(selectedCategories).size()) {
            throw validationError("Choose categories from the available options.");
        }
    }

    public void validatePriorities(Set<String> shoppingPriorities) {
        if (shoppingPriorities == null || shoppingPriorities.isEmpty()) {
            throw validationError("Choose at least one priority.");
        }
        if (shoppingPriorities.size() > MAX_PRIORITIES) {
            throw validationError("Choose up to 4 priorities.");
        }
        if (!PRIORITY_OPTIONS.containsAll(shoppingPriorities)) {
            throw validationError("Choose priorities from the available options.");
        }
    }

    private void validate(UserPreference preferences) {
        if (preferences == null) {
            throw validationError("Preferences are required.");
        }
        validateCategories(preferences.selectedCategories());
        validatePriorities(preferences.shoppingPriorities());
        if (preferences.shoppingStyle() == null) {
            throw validationError("Choose a shopping style.");
        }
    }

    private PreferenceException validationError(String message) {
        return new PreferenceException(PreferenceException.Code.VALIDATION, message);
    }

    private PreferenceException persistenceError(String message, Throwable cause) {
        return new PreferenceException(PreferenceException.Code.PERSISTENCE, message, cause);
    }
}
