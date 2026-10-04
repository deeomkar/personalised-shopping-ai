package com.myshop.navigation;

import com.myshop.component.AppShell;
import com.myshop.controller.AuthController;
import com.myshop.model.User;
import com.myshop.model.UserPreference;
import com.myshop.service.PreferenceService;
import com.myshop.service.SearchService;
import com.myshop.service.CompareService;
import com.myshop.service.HistoryService;
import com.myshop.service.SavedProductService;
import com.myshop.session.SessionManager;
import com.myshop.view.AuthView;
import com.myshop.view.OnboardingView;
import javafx.scene.layout.StackPane;

public final class ApplicationRouter extends StackPane {

    private final AuthController authController;
    private final PreferenceService preferenceService;
    private final SearchService searchService;
    private final SessionManager sessionManager;
    private final SavedProductService savedProductService;
    private final CompareService compareService;
    private final HistoryService historyService;

    public ApplicationRouter(
            AuthController authController,
            PreferenceService preferenceService,
            SearchService searchService,
            SessionManager sessionManager
    ) {
        this(authController, preferenceService, searchService, sessionManager, null, null, null);
    }

    public ApplicationRouter(
            AuthController authController,
            PreferenceService preferenceService,
            SearchService searchService,
            SessionManager sessionManager,
            SavedProductService savedProductService,
            CompareService compareService,
            HistoryService historyService
    ) {
        this.authController = authController;
        this.preferenceService = preferenceService;
        this.searchService = searchService;
        this.sessionManager = sessionManager;
        this.savedProductService = savedProductService;
        this.compareService = compareService;
        this.historyService = historyService;
        getStyleClass().add("application-router");
        showAuth();
    }

    public void showAuth() {
        getChildren().setAll(new AuthView(authController, this::showAuthenticatedApp));
    }

    public void showAuthenticatedApp(User user) {
        sessionManager.authenticate(user);
        if (preferenceService.hasCompletedOnboarding(user.id())) {
            showAppShell(user, preferenceService.load(user.id()).orElseThrow());
        } else {
            showOnboarding(user);
        }
    }

    private void showOnboarding(User user) {
        getChildren().setAll(new OnboardingView(
                preferenceService,
                user,
                preferences -> showAppShell(user, preferences)
        ));
    }

    private void showAppShell(User user, UserPreference userPreference) {
        getChildren().setAll(new AppShell(user, userPreference, searchService, this::logout,
                savedProductService, compareService, historyService, preferenceService));
    }

    public void logout() {
        if (compareService != null) compareService.clear();
        sessionManager.clear();
        showAuth();
    }
}
