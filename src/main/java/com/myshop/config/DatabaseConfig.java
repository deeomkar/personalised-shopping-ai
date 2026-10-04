package com.myshop.config;

import java.nio.file.Path;

public final class DatabaseConfig {

    private DatabaseConfig() {
    }

    public static Path defaultDatabasePath() {
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", "MyShop", "myshop.db");
        }
        if (os.contains("win")) {
            String appData = System.getenv("LOCALAPPDATA");
            String base = appData == null || appData.isBlank() ? home : appData;
            return Path.of(base, "MyShop", "myshop.db");
        }
        return Path.of(home, ".myshop", "myshop.db");
    }
}
