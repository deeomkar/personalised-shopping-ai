package com.myshop.component;

import com.myshop.model.Product;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public final class ProductArtwork extends StackPane {

    public ProductArtwork(Product product, double height) {
        this(product.artwork(), product.artworkClass(), height, product.imageUrl());
    }

    public ProductArtwork(IconType iconType, String artworkClass, double height) {
        this(iconType, artworkClass, height, null);
    }

    private ProductArtwork(IconType iconType, String artworkClass, double height, String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            ImageView image = new ImageView(new Image(imageUrl, true));
            image.setPreserveRatio(true);
            image.setFitHeight(height - 24);
            image.getStyleClass().add("product-image");
            getChildren().add(image);
        } else {
            IconView icon = new IconView(iconType == null ? IconType.BAG : iconType, 58);
            icon.setStrokeColor(Color.web("#F8F6F0"));
            getChildren().add(icon);
        }

        if (imageUrl == null || imageUrl.isBlank()) {
            Label synthetic = new Label("MOCK PREVIEW");
            synthetic.getStyleClass().add("artwork-caption");
            StackPane.setAlignment(synthetic, Pos.BOTTOM_LEFT);
            getChildren().add(synthetic);
        }

        setAlignment(Pos.CENTER);
        setMinHeight(height);
        setPrefHeight(height);
        setMaxHeight(height);
        getStyleClass().addAll("product-artwork", artworkClass);
    }

}
