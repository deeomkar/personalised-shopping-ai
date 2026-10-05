package com.myshop.view;

import com.myshop.component.CategoryCard;
import com.myshop.component.CompactProductCard;
import com.myshop.component.ProductCard;
import com.myshop.component.SearchField;
import com.myshop.component.SectionHeading;
import com.myshop.model.Category;
import com.myshop.model.Product;
import com.myshop.service.MockCatalogService;
import com.myshop.service.HistoryService;
import com.myshop.service.SavedProductService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.function.Consumer;

public final class DiscoverView extends ScrollPane {

    private final FlowPane categoryGrid = new FlowPane();
    private final FlowPane productGrid = new FlowPane();

    private final Consumer<String> onSearch;
    private final Consumer<Category> onCategorySelected;
    private final Consumer<Product> onProductSelected;
    private final SavedProductService savedProductService;
    private final HistoryService historyService;
    private final long userId;

    public DiscoverView(
            Consumer<String> onSearch,
            Consumer<Category> onCategorySelected,
            Consumer<Product> onProductSelected
    ) {
        this(onSearch, onCategorySelected, onProductSelected, null, null, -1);
    }

    public DiscoverView(
            Consumer<String> onSearch,
            Consumer<Category> onCategorySelected,
            Consumer<Product> onProductSelected,
            SavedProductService savedProductService,
            HistoryService historyService,
            long userId
    ) {
        this.onSearch = Objects.requireNonNull(onSearch, "onSearch");
        this.onCategorySelected = Objects.requireNonNull(onCategorySelected, "onCategorySelected");
        this.onProductSelected = Objects.requireNonNull(onProductSelected, "onProductSelected");
        this.savedProductService = savedProductService;
        this.historyService = historyService;
        this.userId = userId;

        VBox content = new VBox();
        content.setPadding(new Insets(44, 48, 72, 48));
        content.setSpacing(42);
        content.getStyleClass().add("discover-content");

        content.getChildren().add(createHero());
        content.getChildren().add(createCategorySection());
        content.getChildren().add(createProductSection());
        content.getChildren().add(createRecentlyViewedSection());

        setContent(content);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        getStyleClass().add("screen-scroll");
        updateWrapLengths(1120);
        viewportBoundsProperty().addListener((observable, oldValue, newValue) ->
                updateWrapLengths(newValue.getWidth()));
    }

    private VBox createHero() {
        Label heading = new Label("Find something you'll actually love.");
        heading.setWrapText(true);
        heading.getStyleClass().add("hero-title");

        Label support = new Label("Search naturally or browse products that fit your preferences.");
        support.setWrapText(true);
        support.getStyleClass().add("hero-supporting");

        SearchField searchField = new SearchField("Search products, brands, or describe what you need");
        Button searchButton = new Button("Search");
        searchButton.setMnemonicParsing(false);
        searchButton.setAccessibleText("Search products");
        searchButton.getStyleClass().addAll("primary-button", "search-submit");
        Runnable submitSearch = () -> {
            String query = searchField.input().getText().trim();
            if (query.isBlank()) {
                searchField.setInvalid(true);
                searchField.input().requestFocus();
                return;
            }
            searchField.setInvalid(false);
            onSearch.accept(query);
        };
        searchField.setOnAction(event -> submitSearch.run());
        searchButton.setOnAction(event -> submitSearch.run());

        HBox searchRow = new HBox(searchField, searchButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setSpacing(12);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchRow.getStyleClass().add("search-row");

        VBox hero = new VBox(heading, support, searchRow);
        hero.setSpacing(15);
        hero.setMaxWidth(800);
        hero.getStyleClass().add("hero-block");
        return hero;
    }

    private VBox createCategorySection() {
        categoryGrid.setHgap(13);
        categoryGrid.setVgap(13);
        categoryGrid.setAlignment(Pos.TOP_LEFT);
        for (Category category : MockCatalogService.categories()) {
            categoryGrid.getChildren().add(new CategoryCard(category, onCategorySelected));
        }

        VBox section = new VBox(new SectionHeading("Shop by category"), categoryGrid);
        section.setSpacing(18);
        section.getStyleClass().add("section-block");
        return section;
    }

    private VBox createProductSection() {
        productGrid.setHgap(18);
        productGrid.setVgap(18);
        productGrid.setAlignment(Pos.TOP_LEFT);
        for (Product product : pickedForYou()) {
            boolean saved = savedProductService != null && savedProductService.isSaved(userId, product.id());
            Product displayProduct = saved == product.saved() ? product : copyWithSaved(product, saved);
            productGrid.getChildren().add(new ProductCard(displayProduct, onProductSelected,
                    (selected, nextSaved) -> { if (savedProductService != null) { if (nextSaved) savedProductService.save(userId, selected); else savedProductService.unsave(userId, selected.id()); } }));
        }

        VBox section = new VBox(
                new SectionHeading("Picked for you", "Your saved and recently viewed products will appear here."),
                productGrid
        );
        section.setSpacing(18);
        section.getStyleClass().add("section-block");
        return section;
    }

    private List<Product> pickedForYou() {
        Map<String, Product> localProducts = new LinkedHashMap<>();
        if (historyService != null) {
            historyService.recentlyViewed(userId).stream()
                    .filter(this::isIndiaSafeProduct)
                    .forEach(product -> localProducts.putIfAbsent(product.id(), product));
        }
        if (savedProductService != null) {
            savedProductService.list(userId).stream()
                    .filter(this::isIndiaSafeProduct)
                    .forEach(product -> localProducts.putIfAbsent(product.id(), product));
        }
        if (!localProducts.isEmpty()) {
            return localProducts.values().stream().limit(4).toList();
        }
        return MockCatalogService.discoveryPlaceholders();
    }

    private boolean isIndiaSafeProduct(Product product) {
        String price = product.price();
        if (price != null && !price.isBlank()
                && !price.contains("₹")
                && !price.toUpperCase(Locale.ROOT).contains("INR")) {
            return false;
        }
        return product.offers().isEmpty() || product.offers().stream()
                .anyMatch(offer -> "INR".equalsIgnoreCase(offer.currency()));
    }

    private VBox createRecentlyViewedSection() {
        HBox recentRow = new HBox();
        recentRow.setSpacing(14);
        List<Product> recentlyViewed = historyService == null ? MockCatalogService.recentlyViewed()
                : historyService.recentlyViewed(userId).stream()
                .filter(this::isIndiaSafeProduct)
                .toList();
        for (Product product : recentlyViewed) {
            recentRow.getChildren().add(new CompactProductCard(product, onProductSelected));
        }

        ScrollPane recentScroll = new ScrollPane(recentRow);
        recentScroll.setFitToHeight(true);
        recentScroll.setFitToWidth(false);
        recentScroll.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        recentScroll.setVbarPolicy(ScrollBarPolicy.NEVER);
        recentScroll.setPrefViewportHeight(98);
        recentScroll.getStyleClass().add("recent-scroll");

        VBox section = new VBox(new SectionHeading("Recently viewed"), recentScroll);
        section.setSpacing(18);
        section.getStyleClass().add("section-block");
        return section;
    }

    private Product copyWithSaved(Product product, boolean saved) {
        return new Product(product.id(), product.brand(), product.name(), product.category(), product.price(),
                product.originalPrice(), product.discount(), product.rating(), product.reviewCount(), product.store(),
                product.artwork(), product.artworkClass(), product.imageUrl(), product.description(), saved,
                product.offers(), product.externalUrl());
    }

    private void updateWrapLengths(double width) {
        double usableWidth = Math.max(640, width - 96);
        categoryGrid.setPrefWrapLength(usableWidth);
        productGrid.setPrefWrapLength(usableWidth);
    }
}
