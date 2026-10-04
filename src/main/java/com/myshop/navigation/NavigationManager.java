package com.myshop.navigation;

import com.myshop.model.Product;
import com.myshop.model.Recommendation;
import com.myshop.model.SearchFilters;
import com.myshop.model.SearchRequest;
import com.myshop.model.SearchSortMode;
import com.myshop.model.User;
import com.myshop.model.UserPreference;
import com.myshop.service.SearchService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class NavigationManager {

    private final StackPane content = new StackPane();
    private final ObjectProperty<NavigationDestination> activeDestination =
            new SimpleObjectProperty<>(NavigationDestination.DISCOVER);
    private final Map<NavigationDestination, Supplier<Node>> factories = new EnumMap<>(NavigationDestination.class);
    private final Map<NavigationDestination, Node> views = new EnumMap<>(NavigationDestination.class);
    private final SearchService searchService;
    private final User user;
    private UserPreference userPreference;
    private SearchRequest searchRequest;
    private Product selectedProduct;
    private Recommendation selectedRecommendation;
    private NavigationDestination detailsReturnDestination = NavigationDestination.DISCOVER;
    private Map<String, Recommendation> recommendations = Map.of();

    public NavigationManager(SearchService searchService, User user, UserPreference userPreference) {
        this.searchService = Objects.requireNonNull(searchService, "searchService");
        this.user = Objects.requireNonNull(user, "user");
        this.userPreference = userPreference;
    }

    public StackPane content() {
        return content;
    }

    public ObjectProperty<NavigationDestination> activeDestinationProperty() {
        return activeDestination;
    }

    public void register(NavigationDestination destination, Supplier<Node> viewFactory) {
        factories.put(destination, viewFactory);
    }

    public void navigateTo(NavigationDestination destination) {
        Supplier<Node> factory = factories.get(destination);
        if (factory == null) {
            throw new IllegalArgumentException("No view registered for " + destination);
        }

        Node view = views.computeIfAbsent(destination, ignored -> factory.get());
        content.getChildren().setAll(view);
        activeDestination.set(destination);
    }

    public void openSearchResults(String query) {
        String normalizedQuery = Objects.requireNonNull(query, "query").trim();
        searchRequest = new SearchRequest(
                normalizedQuery, null, user.id(), userPreference,
                SearchFilters.none(), SearchSortMode.RECOMMENDED
        );
        views.remove(NavigationDestination.SEARCH_RESULTS);
        navigateTo(NavigationDestination.SEARCH_RESULTS);
    }

    public void openCategoryResults(String category) {
        String normalizedCategory = Objects.requireNonNull(category, "category").trim();
        searchRequest = new SearchRequest(
                "", normalizedCategory, user.id(), userPreference,
                SearchFilters.none(), SearchSortMode.RECOMMENDED
        );
        views.remove(NavigationDestination.SEARCH_RESULTS);
        navigateTo(NavigationDestination.SEARCH_RESULTS);
    }

    public void openProduct(Product product) {
        selectedProduct = Objects.requireNonNull(product, "product");
        selectedRecommendation = recommendations.get(product.id());
        detailsReturnDestination = activeDestination.get();
        views.remove(NavigationDestination.PRODUCT_DETAILS);
        navigateTo(NavigationDestination.PRODUCT_DETAILS);
    }

    public void setRecommendations(List<Recommendation> values) {
        recommendations = values == null ? Map.of() : values.stream()
                .collect(java.util.stream.Collectors.toMap(item -> item.product().id(), item -> item,
                        (left, right) -> left));
    }

    public void setUserPreference(UserPreference userPreference) {
        this.userPreference = userPreference;
    }

    public void backFromProduct() {
        navigateTo(detailsReturnDestination);
    }

    public SearchService searchService() {
        return searchService;
    }

    public SearchRequest searchRequest() {
        if (searchRequest == null) {
            throw new IllegalStateException("Search results have not been opened");
        }
        return searchRequest;
    }

    public Product selectedProduct() {
        return selectedProduct;
    }

    public Recommendation selectedRecommendation() {
        return selectedRecommendation;
    }
}
