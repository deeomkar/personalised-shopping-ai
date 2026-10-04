package com.myshop.component;

import com.myshop.navigation.NavigationDestination;
import com.myshop.navigation.NavigationManager;
import com.myshop.model.User;
import com.myshop.model.UserPreference;
import com.myshop.service.SearchService;
import com.myshop.service.CompareService;
import com.myshop.service.HistoryService;
import com.myshop.service.SavedProductService;
import com.myshop.view.CompareView;
import com.myshop.view.DiscoverView;
import com.myshop.view.ProductDetailsView;
import com.myshop.view.SearchResultsView;
import com.myshop.view.SavedView;
import com.myshop.view.ProfileView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

import java.util.EnumMap;
import java.util.Map;

public final class AppShell extends BorderPane {

    private final NavigationManager navigationManager;
    private final Map<NavigationDestination, NavigationItem> navigationItems =
            new EnumMap<>(NavigationDestination.class);

    public AppShell(
            User user,
            UserPreference userPreference,
            SearchService searchService,
            Runnable onLogout
    ) {
        this(user, userPreference, searchService, onLogout, null, null, null, null);
    }

    public AppShell(
            User user,
            UserPreference userPreference,
            SearchService searchService,
            Runnable onLogout,
            SavedProductService savedService,
            CompareService compareService,
            HistoryService historyService,
            com.myshop.service.PreferenceService preferenceService
    ) {
        navigationManager = new NavigationManager(searchService, user, userPreference);
        getStyleClass().add("app-shell");
        setTop(createTopBar(user, onLogout));

        navigationManager.register(NavigationDestination.DISCOVER, () -> new DiscoverView(
                navigationManager::openSearchResults,
                category -> navigationManager.openCategoryResults(category.name()),
                navigationManager::openProduct, savedService, historyService, user.id()
        ));
        navigationManager.register(NavigationDestination.SAVED,
                () -> new SavedView(savedService, user.id(), navigationManager::openProduct,
                        () -> navigationManager.navigateTo(NavigationDestination.DISCOVER)));
        navigationManager.register(NavigationDestination.COMPARE,
                () -> new CompareView(compareService, navigationManager::openProduct,
                        () -> navigationManager.navigateTo(NavigationDestination.DISCOVER)));
        navigationManager.register(NavigationDestination.SEARCH_RESULTS, () -> new SearchResultsView(
                navigationManager.searchService(),
                navigationManager.searchRequest(),
                navigationManager::openProduct,
                () -> navigationManager.navigateTo(NavigationDestination.DISCOVER),
                navigationManager::setRecommendations,
                savedService, user.id()
        ));
        navigationManager.register(NavigationDestination.PRODUCT_DETAILS, () -> new ProductDetailsView(
                navigationManager.selectedProduct(),
                navigationManager.selectedRecommendation(),
                navigationManager::backFromProduct,
                savedService == null ? null : () -> savedService.toggle(user.id(), navigationManager.selectedProduct()),
                savedService == null ? () -> false : () -> savedService.isSaved(user.id(), navigationManager.selectedProduct().id()),
                compareService == null ? null : () -> { compareService.add(navigationManager.selectedProduct()); navigationManager.navigateTo(NavigationDestination.COMPARE); },
                historyService == null ? null : () -> historyService.recordViewedProduct(user.id(), navigationManager.selectedProduct()),
                null
        ));
        if (preferenceService != null) {
            navigationManager.register(NavigationDestination.PROFILE, () -> new ProfileView(
                    user, userPreference, preferenceService, historyService, navigationManager::setUserPreference
            ));
        }

        StackPane content = navigationManager.content();
        content.getStyleClass().add("shell-content");
        setCenter(content);

        navigationManager.activeDestinationProperty().addListener((observable, oldValue, newValue) ->
                updateActiveNavigation(newValue));
        navigationManager.navigateTo(NavigationDestination.DISCOVER);
    }

    private HBox createTopBar(User user, Runnable onLogout) {
        Label brand = new Label("MyShop");
        brand.getStyleClass().add("brand");

        HBox brandLockup = new HBox(new MyShopLogo(28), brand);
        brandLockup.setAlignment(Pos.CENTER_LEFT);
        brandLockup.setSpacing(10);
        brandLockup.getStyleClass().add("brand-lockup");

        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setSpacing(4);
        for (NavigationDestination destination : NavigationDestination.values()) {
            if (!destination.primary()) {
                continue;
            }
            NavigationItem item = new NavigationItem(destination);
            item.setOnAction(event -> navigationManager.navigateTo(destination));
            navigationItems.put(destination, item);
            nav.getChildren().add(item);
        }

        HBox left = new HBox(brandLockup, nav);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setSpacing(42);

        IconView profileIcon = new IconView(IconType.USER, 15);
        StackPane profileAvatar = new StackPane(profileIcon);
        profileAvatar.getStyleClass().add("profile-avatar");
        MenuButton profile = new MenuButton(user.name(), profileAvatar);
        profile.setMnemonicParsing(false);
        profile.setAccessibleText("Profile for " + user.name());
        MenuItem profileItem = new MenuItem("Profile & settings");
        profileItem.setOnAction(event -> navigationManager.navigateTo(NavigationDestination.PROFILE));
        MenuItem logout = new MenuItem("Log out");
        logout.setOnAction(event -> onLogout.run());
        profile.getItems().addAll(profileItem, new SeparatorMenuItem(), logout);
        profile.getStyleClass().add("profile-chip");

        HBox bar = new HBox(left, profile);
        HBox.setHgrow(left, Priority.ALWAYS);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 34, 0, 42));
        bar.setMinHeight(76);
        bar.setPrefHeight(76);
        bar.getStyleClass().add("top-bar");
        return bar;
    }

    private void updateActiveNavigation(NavigationDestination active) {
        navigationItems.forEach((destination, item) -> item.setActive(destination == active));
    }
}
