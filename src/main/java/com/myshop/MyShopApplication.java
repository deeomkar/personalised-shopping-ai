package com.myshop;

import com.myshop.config.DatabaseConfig;
import com.myshop.controller.AuthController;
import com.myshop.navigation.ApplicationRouter;
import com.myshop.repository.DatabaseManager;
import com.myshop.repository.SqliteUserRepository;
import com.myshop.service.AuthService;
import com.myshop.repository.SqliteUserPreferenceRepository;
import com.myshop.service.PreferenceService;
import com.myshop.service.ProductSearchProviderFactory;
import com.myshop.service.SearchService;
import com.myshop.service.HistoryService;
import com.myshop.service.SavedProductService;
import com.myshop.service.CompareService;
import com.myshop.repository.SqliteHistoryRepository;
import com.myshop.repository.SqliteSavedProductRepository;
import com.myshop.service.RecommendationService;
import com.myshop.service.ShoppingIntentProviderChain;
import com.myshop.security.BCryptPasswordHasher;
import com.myshop.session.SessionManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MyShopApplication extends Application {

    @Override
    public void start(Stage stage) {
        DatabaseManager database = new DatabaseManager(DatabaseConfig.defaultDatabasePath());
        database.initialize();
        AuthService authService = new AuthService(
                new SqliteUserRepository(database),
                new BCryptPasswordHasher()
        );
        PreferenceService preferenceService = new PreferenceService(
                new SqliteUserPreferenceRepository(database)
        );
        HistoryService historyService = new HistoryService(new SqliteHistoryRepository(database));
        SavedProductService savedProductService = new SavedProductService(new SqliteSavedProductRepository(database));
        CompareService compareService = new CompareService();
        SearchService searchService = new SearchService(
                ProductSearchProviderFactory.fromEnvironment(), new ShoppingIntentProviderChain(),
                preferenceService, new RecommendationService(), historyService
        );
        ApplicationRouter router = new ApplicationRouter(
                new AuthController(authService), preferenceService, searchService, new SessionManager(),
                savedProductService, compareService, historyService
        );
        Scene scene = new Scene(router, 1240, 840);
        scene.getStylesheets().addAll(
                getClass().getResource("/com/myshop/css/base.css").toExternalForm(),
                getClass().getResource("/com/myshop/css/components.css").toExternalForm(),
                getClass().getResource("/com/myshop/css/screens.css").toExternalForm(),
                getClass().getResource("/com/myshop/css/auth.css").toExternalForm(),
                getClass().getResource("/com/myshop/css/onboarding.css").toExternalForm()
        );

        stage.setTitle("MyShop");
        stage.setMinWidth(920);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
