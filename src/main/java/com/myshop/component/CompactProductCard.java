package com.myshop.component;

import com.myshop.model.Product;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.function.Consumer;

public final class CompactProductCard extends HBox {

    public CompactProductCard(Product product) {
        this(product, ignored -> {
        });
    }

    public CompactProductCard(Product product, Consumer<Product> onOpen) {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(onOpen, "onOpen");

        ProductArtwork artwork = new ProductArtwork(product, 72);
        artwork.setMinWidth(72);
        artwork.setPrefWidth(72);
        artwork.setMaxWidth(72);

        Label brand = new Label(text(product.brand()).toUpperCase());
        brand.getStyleClass().add("compact-brand");
        Label name = new Label(product.name());
        name.setWrapText(true);
        name.getStyleClass().add("compact-name");

        VBox details = new VBox(brand, name);
        if (!text(product.price()).isBlank()) {
            Label price = new Label(product.price());
            price.getStyleClass().add("compact-price");
            details.getChildren().add(price);
        }
        details.setSpacing(4);
        details.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(artwork, details);
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setMinWidth(250);
        setPrefWidth(285);
        setMaxWidth(340);
        getStyleClass().add("compact-product-card");
        setAccessibleText(product.brand() + " " + product.name());
        setFocusTraversable(true);
        setOnMouseClicked(event -> onOpen.accept(product));
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                onOpen.accept(product);
                event.consume();
            }
        });
    }

    private String text(String value) { return value == null ? "" : value; }
}
