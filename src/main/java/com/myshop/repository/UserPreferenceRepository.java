package com.myshop.repository;

import com.myshop.model.UserPreference;

import java.util.Optional;

public interface UserPreferenceRepository {

    Optional<UserPreference> findByUserId(long userId);

    UserPreference save(UserPreference preferences);

    boolean hasCompletedOnboarding(long userId);
}
