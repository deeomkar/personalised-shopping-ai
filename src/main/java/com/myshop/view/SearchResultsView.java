package com.myshop.view;

import com.myshop.component.IconType;
import com.myshop.component.IconView;
import com.myshop.component.ProductCard;
import com.myshop.component.SearchField;
import com.myshop.model.Product;
import com.myshop.model.Recommendation;
import com.myshop.model.SearchFilters;
import com.myshop.model.SearchRequest;
import com.myshop.model.SearchResult;
import com.myshop.model.SearchSortMode;
import com.myshop.service.MockCatalogService;
import com.myshop.service.SearchService;
import com.myshop.service.SavedProductService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SearchResultsView extends ScrollPane {

    private static final String ANY_CATEGORY = "Any category";
    private static final String ANY_RATING = "Any rating";

    private final SearchService searchService;
    private final Consumer<Product> onProductSelected;
    private final Runnable backAction;
    private final Consumer<List<Recommendation>> onRecommendations;
    private final SavedProductService savedService;
    private final long userId;
    private final Label resultHeading = new Label();
    private final Label resultCount = new Label();
    private final SearchField queryField = new SearchField("Search products, brands, or describe what you need");
    private final ChoiceBox<String> categoryChoice = new ChoiceBox<>();
    private final TextField brandField = new TextField();
    private final ChoiceBox<String> ratingChoice = new ChoiceBox<>();
    private final TextField minimumPriceField = new TextField();
    private final TextField maximumPriceField = new TextField();
    private final ChoiceBox<SearchSortMode> sortChoice = new ChoiceBox<>();
    private final Label filterError = new Label();
    private final StackPane resultHost = new StackPane();
    private SearchRequest request;
    private SearchResult lastResult;

    public SearchResultsView(
            SearchService searchService,
            SearchRequest request,
            Consumer<Product> onProductSelected,
            Runnable backAction
    ) {
        this(searchService, request, onProductSelected, backAction, ignored -> { });
    }

    public SearchResultsView(
            SearchService searchService,
            SearchRequest request,
            Consumer<Product> onProductSelected,
            Runnable backAction,
            Consumer<List<Recommendation>> onRecommendations
    ) {
        this(searchService, request, onProductSelected, backAction, onRecommendations, null, -1);
    }

    public SearchResultsView(
            SearchService searchService,
            SearchRequest request,
            Consumer<Product> onProductSelected,
            Runnable backAction,
            Consumer<List<Recommendation>> onRecommendations,
            SavedProductService savedService,
            long userId
    ) {
        this.searchService = Objects.requireNonNull(searchService, "searchService");
        this.request = Objects.requireNonNull(request, "request");
        this.onProductSelected = Objects.requireNonNull(onProductSelected, "onProductSelected");
        this.backAction = Objects.requireNonNull(backAction, "backAction");
        this.onRecommendations = Objects.requireNonNull(onRecommendations, "onRecommendations");
        this.savedService = savedService;
        this.userId = userId;

        setContent(createPage());
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        getStyleClass().add("screen-scroll");
        executeSearch();
    }

    private VBox createPage() {
        Button back = new Button("Back to Discover");
        back.setMnemonicParsing(false);
        IconView arrow = new IconView(IconType.ARROW_RIGHT, 16);
        arrow.setRotate(180);
        back.setGraphic(arrow);
        back.setOnAction(event -> backAction.run());
        back.getStyleClass().add("secondary-button");

        resultHeading.setWrapText(true);
        resultHeading.getStyleClass().add("results-title");
        resultCount.getStyleClass().add("results-count");

        VBox headingCopy = new VBox(resultHeading, resultCount);
        headingCopy.setSpacing(6);
        HBox heading = new HBox(back, headingCopy);
        heading.setAlignment(Pos.CENTER_LEFT);
        heading.setSpacing(22);
        heading.getStyleClass().add("results-heading-row");

        queryField.input().setText(displayQuery(request));
        Button searchButton = new Button("Search");
        searchButton.setMnemonicParsing(false);
        searchButton.getStyleClass().addAll("primary-button", "search-submit");
        queryField.setOnAction(event -> submitQuery());
        searchButton.setOnAction(event -> submitQuery());
        HBox searchRow = new HBox(queryField, searchButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setSpacing(12);
        HBox.setHgrow(queryField, Priority.ALWAYS);
        searchRow.getStyleClass().add("results-search-row");

        configureFilters();
        HBox filters = new HBox(
                labelledControl("Category", categoryChoice),
                labelledControl("Brand", brandField),
                labelledControl("Rating", ratingChoice),
                labelledControl("Min price", minimumPriceField),
                labelledControl("Max price", maximumPriceField),
                createSortControl()
        );
        filters.setAlignment(Pos.BOTTOM_LEFT);
        filters.setSpacing(10);
        filters.getStyleClass().add("results-filters");

        Button applyFilters = new Button("Apply filters");
        applyFilters.setMnemonicParsing(false);
        applyFilters.setOnAction(event -> applyCurrentControls());
        applyFilters.getStyleClass().add("secondary-button");

        filterError.setManaged(false);
        filterError.setVisible(false);
        filterError.getStyleClass().add("results-filter-error");

        resultHost.setMinHeight(460);
        resultHost.setMaxWidth(1200);
        resultHost.getStyleClass().add("results-host");

        VBox page = new VBox(heading, searchRow, filters, applyFilters, filterError, resultHost);
        page.setPadding(new Insets(44, 48, 72, 48));
        page.setSpacing(18);
        page.getStyleClass().add("results-page");
        return page;
    }

    private void configureFilters() {
        categoryChoice.getItems().add(ANY_CATEGORY);
        MockCatalogService.categories().forEach(category -> categoryChoice.getItems().add(category.name()));
        categoryChoice.setValue(request.filters().category() == null
                ? request.category() == null ? ANY_CATEGORY : request.category()
                : request.filters().category());
        categoryChoice.getStyleClass().add("results-choice");

        brandField.setPromptText("Any brand");
        brandField.getStyleClass().add("results-filter-input");
        brandField.setPrefWidth(130);

        ratingChoice.getItems().addAll(ANY_RATING, "4.0+", "4.5+");
        ratingChoice.setValue(ratingLabel(request.filters().minimumRating()));
        ratingChoice.getStyleClass().add("results-choice");

        minimumPriceField.setPromptText("₹ min");
        minimumPriceField.getStyleClass().add("results-filter-input");
        minimumPriceField.setPrefWidth(78);
        maximumPriceField.setPromptText("₹ max");
        maximumPriceField.getStyleClass().add("results-filter-input");
        maximumPriceField.setPrefWidth(78);

        sortChoice.getItems().addAll(SearchSortMode.values());
        sortChoice.setValue(request.sortMode());
        sortChoice.getStyleClass().add("results-choice");
        sortChoice.setOnAction(event -> applyCurrentControls());
    }

    private VBox labelledControl(String labelText, javafx.scene.Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("results-filter-label");
        VBox wrapper = new VBox(label, control);
        wrapper.setSpacing(5);
        wrapper.getStyleClass().add("results-filter-control");
        return wrapper;
    }

    private VBox createSortControl() {
        Label label = new Label("Sort");
        label.getStyleClass().add("results-filter-label");
        VBox wrapper = new VBox(label, sortChoice);
        wrapper.setSpacing(5);
        wrapper.getStyleClass().add("results-sort-control");
        return wrapper;
    }

    private void submitQuery() {
        String query = queryField.input().getText().trim();
        if (query.isBlank()) {
            queryField.setInvalid(true);
            queryField.input().requestFocus();
            return;
        }
        queryField.setInvalid(false);
        SearchFilters filters = readFilters();
        if (filters == null) {
            return;
        }
        request = new SearchRequest(
                query, null, request.userId(), request.userPreference(), filters, sortChoice.getValue()
        );
        executeSearch();
    }

    private void applyCurrentControls() {
        SearchFilters filters = readFilters();
        if (filters == null) {
            return;
        }
        request = request.withFilters(filters).withSortMode(sortChoice.getValue());
        executeSearch();
    }

    private SearchFilters readFilters() {
        clearFilterError();
        try {
            String category = ANY_CATEGORY.equals(categoryChoice.getValue())
                    ? null : categoryChoice.getValue();
            String brand = brandField.getText();
            Double minimum = parsePrice(minimumPriceField.getText());
            Double maximum = parsePrice(maximumPriceField.getText());
            double rating = switch (ratingChoice.getValue()) {
                case "4.0+" -> 4.0;
                case "4.5+" -> 4.5;
                default -> 0;
            };
            return new SearchFilters(category, brand, minimum, maximum, rating);
        } catch (IllegalArgumentException exception) {
            showFilterError("Enter a valid price range.");
            return null;
        }
    }

    private Double parsePrice(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Double.parseDouble(value.replace("$", "").replace("₹", "").replace(",", "").trim());
    }

    private void executeSearch() {
        showLoading();
        SearchRequest searchRequest = request;
        Task<SearchResult> task = new Task<>() {
            @Override
            protected SearchResult call() {
                return searchService.search(searchRequest);
            }
        };
        task.setOnSucceeded(event -> showResult(task.getValue()));
        task.setOnFailed(event -> showError("We couldn't load products right now."));
        Thread thread = new Thread(task, "myshop-search-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void showResult(SearchResult result) {
        lastResult = result;
        onRecommendations.accept(result.recommendations());
        request = result.request();
        resultHeading.setText(headingFor(request));
        resultCount.setText(result.totalResultCount() + (result.totalResultCount() == 1 ? " result" : " results"));
        if (result.state() == SearchResult.SearchState.ERROR) {
            showError(result.message());
        } else if (result.isEmpty()) {
            showEmpty();
        } else {
            showProducts(result.products());
        }
    }

    private void showProducts(List<Product> products) {
        FlowPane grid = new FlowPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.setPrefWrapLength(1100);
        grid.getStyleClass().add("results-product-grid");
        Map<String, Recommendation> recommendations = currentRecommendations();
        for (Product product : products) {
            boolean saved = savedService != null && savedService.isSaved(userId, product.id());
            Product displayProduct = copyWithSaved(product, saved);
            grid.getChildren().add(new ProductCard(displayProduct, recommendations.get(product.id()), onProductSelected,
                    (selected, nextSaved) -> { if (savedService != null) { if (nextSaved) savedService.save(userId, selected); else savedService.unsave(userId, selected.id()); } }));
        }
        resultHost.getChildren().setAll(grid);
    }

    private Product copyWithSaved(Product product, boolean saved) {
        if (product.saved() == saved) return product;
        return new Product(product.id(), product.brand(), product.name(), product.category(), product.price(),
                product.originalPrice(), product.discount(), product.rating(), product.reviewCount(), product.store(),
                product.artwork(), product.artworkClass(), product.imageUrl(), product.description(), saved,
                product.offers(), product.externalUrl());
    }

    private Map<String, Recommendation> currentRecommendations() {
        // SearchResult keeps products backward-compatible while carrying explanations
        // for this screen when the recommendation layer is active.
        return lastResult == null ? Map.of() : lastResult.recommendations().stream()
                .collect(Collectors.toMap(recommendation -> recommendation.product().id(), Function.identity()));
    }

    private void showLoading() {
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(30, 30);
        Label message = new Label("Finding products...");
        message.getStyleClass().add("results-state-message");
        VBox loading = new VBox(progress, message);
        loading.setAlignment(Pos.CENTER);
        loading.setSpacing(14);
        resultHost.getChildren().setAll(loading);
    }

    private void showEmpty() {
        IconView icon = new IconView(IconType.SEARCH, 28);
        icon.setStrokeColor(javafx.scene.paint.Color.web("#1F4D3B"));
        StackPane iconTile = new StackPane(icon);
        iconTile.getStyleClass().add("results-icon-tile");
        Label title = new Label("No products found");
        title.getStyleClass().add("results-empty-title");
        Label message = new Label("Try adjusting your search or filters.");
        message.getStyleClass().add("results-state-message");
        Button clear = new Button("Clear filters");
        clear.setMnemonicParsing(false);
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(event -> clearFilters());
        VBox empty = new VBox(iconTile, title, message, clear);
        empty.setAlignment(Pos.CENTER);
        empty.setSpacing(14);
        resultHost.getChildren().setAll(empty);
    }

    private void showError(String message) {
        Label title = new Label("Search unavailable");
        title.getStyleClass().add("results-empty-title");
        Label detail = new Label(message);
        detail.setWrapText(true);
        detail.getStyleClass().add("results-state-message");
        Button retry = new Button("Retry");
        retry.setMnemonicParsing(false);
        retry.getStyleClass().add("secondary-button");
        retry.setOnAction(event -> executeSearch());
        VBox error = new VBox(title, detail, retry);
        error.setAlignment(Pos.CENTER);
        error.setSpacing(14);
        resultHost.getChildren().setAll(error);
    }

    private void clearFilters() {
        categoryChoice.setValue(ANY_CATEGORY);
        brandField.clear();
        ratingChoice.setValue(ANY_RATING);
        minimumPriceField.clear();
        maximumPriceField.clear();
        clearFilterError();
        request = request.withFilters(SearchFilters.none());
        executeSearch();
    }

    private void showFilterError(String message) {
        filterError.setText(message);
        filterError.setManaged(true);
        filterError.setVisible(true);
    }

    private void clearFilterError() {
        filterError.setText("");
        filterError.setManaged(false);
        filterError.setVisible(false);
    }

    private String displayQuery(SearchRequest searchRequest) {
        return searchRequest.rawQuery().isBlank() ? searchRequest.category() : searchRequest.rawQuery();
    }

    private String headingFor(SearchRequest searchRequest) {
        String value = searchRequest.rawQuery().isBlank()
                ? searchRequest.category()
                : searchRequest.rawQuery();
        return "Results for \"" + value + "\"";
    }

    private String ratingLabel(double rating) {
        if (rating >= 4.5) {
            return "4.5+";
        }
        if (rating >= 4.0) {
            return "4.0+";
        }
        return ANY_RATING;
    }
}
