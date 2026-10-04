package com.myshop.view;

import com.myshop.component.EmptyState;
import com.myshop.component.IconType;
import com.myshop.component.ProductCard;
import com.myshop.model.Product;
import com.myshop.service.SavedProductService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class SavedView extends ScrollPane {
    private final SavedProductService savedService;
    private final long userId;
    private final Consumer<Product> onOpen;
    private final Runnable browseAction;

    public SavedView(Runnable browseAction) {
        this(null, -1, ignored -> { }, browseAction);
    }

    public SavedView(SavedProductService savedService, long userId, Consumer<Product> onOpen, Runnable browseAction) {
        this.savedService = savedService; this.userId = userId; this.onOpen = Objects.requireNonNull(onOpen, "onOpen");
        this.browseAction = Objects.requireNonNull(browseAction, "browseAction");
        setFitToWidth(true); setHbarPolicy(ScrollBarPolicy.NEVER); setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        getStyleClass().add("screen-scroll"); refresh();
    }

    public void refresh() {
        List<Product> products = savedService == null ? List.of() : savedService.list(userId);
        StackPane content = new StackPane(products.isEmpty() ? createEmpty() : createGrid(products));
        content.setPadding(new Insets(44, 48, 72, 48)); content.setMinHeight(680); content.getStyleClass().add("empty-screen-content");
        setContent(content);
    }

    private EmptyState createEmpty() {
        return new EmptyState(IconType.SAVED, "Saved", "Products you save will appear here.", "Explore products", browseAction);
    }

    private VBox createGrid(List<Product> products) {
        FlowPane grid = new FlowPane(); grid.setHgap(18); grid.setVgap(18); grid.setPrefWrapLength(1100); grid.getStyleClass().add("results-product-grid");
        for (Product product : products) grid.getChildren().add(new ProductCard(product, onOpen, (saved, next) -> refresh()));
        VBox content = new VBox(grid); content.setAlignment(Pos.TOP_LEFT); content.setSpacing(18);
        return content;
    }
}
