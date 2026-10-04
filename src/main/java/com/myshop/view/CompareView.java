package com.myshop.view;

import com.myshop.component.EmptyState;
import com.myshop.component.IconType;
import com.myshop.component.ProductArtwork;
import com.myshop.component.RatingView;
import com.myshop.model.Product;
import com.myshop.service.CompareService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public final class CompareView extends ScrollPane {
    private final CompareService compareService;
    private final Consumer<Product> onOpen;
    private final Runnable browseAction;

    public CompareView(Runnable browseAction) { this(null, ignored -> { }, browseAction); }

    public CompareView(CompareService compareService, Consumer<Product> onOpen, Runnable browseAction) {
        this.compareService = compareService; this.onOpen = onOpen; this.browseAction = browseAction;
        setFitToWidth(true); setHbarPolicy(ScrollBarPolicy.NEVER); setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        getStyleClass().add("screen-scroll"); refresh();
    }

    public void refresh() {
        List<Product> products = compareService == null ? List.of() : compareService.products();
        VBox page = new VBox(); page.setSpacing(22); page.setPadding(new Insets(44, 48, 72, 48)); page.getStyleClass().add("compare-page");
        if (products.isEmpty()) page.getChildren().add(new EmptyState(IconType.COMPARE, "Compare", "Select products to compare them side by side.", "Browse products", browseAction));
        else {
            HBox header = new HBox(new Label("Compare products")); header.getStyleClass().add("compare-header");
            Button clear = new Button("Clear all"); clear.getStyleClass().add("secondary-button"); clear.setOnAction(event -> { compareService.clear(); refresh(); });
            HBox.setHgrow(header.getChildren().getFirst(), Priority.ALWAYS); header.getChildren().add(clear); page.getChildren().add(header);
            HBox columns = new HBox(); columns.setSpacing(16); columns.getStyleClass().add("compare-columns");
            products.forEach(product -> columns.getChildren().add(column(product)));
            page.getChildren().add(columns);
        }
        setContent(page);
    }

    private VBox column(Product product) {
        ProductArtwork artwork = new ProductArtwork(product, 190); artwork.setPrefWidth(250);
        Label brand = new Label(product.brand() == null ? "" : product.brand().toUpperCase()); brand.getStyleClass().add("detail-brand");
        Label name = new Label(product.name()); name.setWrapText(true); name.getStyleClass().add("detail-name");
        Label price = new Label(product.price() == null ? "" : product.price()); price.getStyleClass().add("product-price");
        VBox card = new VBox(artwork, brand, name, price); card.setSpacing(10); card.setPrefWidth(270); card.getStyleClass().add("compare-card");
        if (Double.isFinite(product.rating()) && product.rating() > 0) card.getChildren().add(new RatingView(product.rating(), product.reviewCount()));
        Button details = new Button("View details"); details.getStyleClass().add("secondary-button"); details.setOnAction(event -> onOpen.accept(product));
        Button remove = new Button("Remove"); remove.getStyleClass().add("text-button"); remove.setOnAction(event -> { compareService.remove(product.id()); refresh(); });
        HBox actions = new HBox(details, remove); actions.setSpacing(8); card.getChildren().add(actions);
        return card;
    }
}
