package com.myshop.component;

import com.myshop.model.Product;
import com.myshop.model.Recommendation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

public final class ProductCard extends VBox {

    private final IconButton saveButton;
    private boolean saved;

    public ProductCard(Product product) {
        this(product, ignored -> {
        });
    }

    public ProductCard(Product product, Consumer<Product> onOpen) {
        this(product, null, onOpen, (ignored, saved) -> { });
    }

    public ProductCard(Product product, Recommendation recommendation, Consumer<Product> onOpen) {
        this(product, recommendation, onOpen, (ignored, saved) -> { });
    }

    public ProductCard(Product product, Consumer<Product> onOpen, BiConsumer<Product, Boolean> onSavedChanged) {
        this(product, null, onOpen, onSavedChanged);
    }

    public ProductCard(Product product, Recommendation recommendation, Consumer<Product> onOpen,
                       BiConsumer<Product, Boolean> onSavedChanged) {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(onOpen, "onOpen");
        Objects.requireNonNull(onSavedChanged, "onSavedChanged");
        saved = product.saved();

        ProductArtwork artwork = new ProductArtwork(product, 172);
        saveButton = new IconButton(IconType.HEART, "Save " + product.name());
        saveButton.setOnAction(event -> { toggleSaved(); onSavedChanged.accept(product, saved); });
        saveButton.addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
        StackPane.setAlignment(saveButton, Pos.TOP_RIGHT);
        StackPane.setMargin(saveButton, new javafx.geometry.Insets(12));
        artwork.getChildren().add(saveButton);

        Label brand = new Label(text(product.brand()).toUpperCase());
        brand.getStyleClass().add("product-brand");

        Label name = new Label(product.name());
        name.setWrapText(true);
        name.setMaxHeight(42);
        name.getStyleClass().add("product-name");

        PriceView price = new PriceView(product.price(), product.originalPrice(), product.discount());
        HBox meta = new HBox();
        if (Double.isFinite(product.rating()) && product.rating() > 0) meta.getChildren().add(new RatingView(product.rating(), product.reviewCount()));
        if (!text(product.store()).isBlank()) { Label store = new Label("From " + product.store()); store.getStyleClass().add("product-store"); meta.getChildren().add(store); }
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.setSpacing(8);

        VBox details = new VBox(brand, name);
        if (!text(product.price()).isBlank() || !text(product.originalPrice()).isBlank()) details.getChildren().add(price);
        if (!meta.getChildren().isEmpty()) details.getChildren().add(meta);
        if (recommendation != null && !recommendation.reasons().isEmpty()) {
            details.getChildren().add(compactRecommendation(recommendation));
        }
        details.setSpacing(7);
        details.getStyleClass().add("product-details");

        getChildren().addAll(artwork, details);
        setSpacing(0);
        getStyleClass().add("product-card");
        setAccessibleText(product.brand() + " " + product.name());
        setFocusTraversable(true);
        setOnMouseClicked(event -> onOpen.accept(product));
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                onOpen.accept(product);
                event.consume();
            }
        });
        updateSavedStyle();
    }

    private void toggleSaved() {
        saved = !saved;
        updateSavedStyle();
    }

    private void updateSavedStyle() {
        if (saved) {
            if (!getStyleClass().contains("is-saved")) {
                getStyleClass().add("is-saved");
            }
            saveButton.getStyleClass().add("is-saved");
            saveButton.setIconColor(Color.web("#1F4D3B"));
        } else {
            getStyleClass().remove("is-saved");
            saveButton.getStyleClass().remove("is-saved");
            saveButton.setIconColor(Color.web("#737373"));
        }
    }

    private HBox compactRecommendation(Recommendation recommendation) {
        Label match = new Label(recommendation.matchLabel());
        match.getStyleClass().add("recommendation-label");
        HBox hint = new HBox(match);
        if (!recommendation.reasons().isEmpty()) {
            Label reason = new Label(compactReason(recommendation.reasons().getFirst().text()));
            reason.setWrapText(false);
            reason.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            reason.getStyleClass().add("recommendation-hint");
            hint.getChildren().add(reason);
        }
        hint.setAlignment(Pos.CENTER_LEFT);
        hint.setSpacing(7);
        hint.getStyleClass().add("recommendation-compact");
        return hint;
    }

    private String compactReason(String reason) {
        if (reason == null || reason.isBlank()) return "";
        return reason.replace("Matches your ", "Matches ");
    }

    private String text(String value) { return value == null ? "" : value; }
}
