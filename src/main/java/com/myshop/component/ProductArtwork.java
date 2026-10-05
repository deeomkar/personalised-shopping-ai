package com.myshop.component;

import com.myshop.model.Product;
import com.myshop.service.RemoteImageService;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public final class ProductArtwork extends StackPane {
    private static final RemoteImageService IMAGE_SERVICE = RemoteImageService.shared();

    public ProductArtwork(Product product, double height) {
        this(product.artwork(), product.artworkClass(), height, product.imageUrl());
    }

    public ProductArtwork(IconType iconType, String artworkClass, double height) {
        this(iconType, artworkClass, height, null);
    }

    private ProductArtwork(IconType iconType, String artworkClass, double height, String imageUrl) {
        IconView placeholder = new IconView(iconType == null ? IconType.BAG : iconType, placeholderSize(height));
        placeholder.setStrokeColor(Color.web("#F8F6F0"));
        placeholder.getStyleClass().add("product-image-placeholder");
        getChildren().add(placeholder);

        if (imageUrl != null && !imageUrl.isBlank()) {
            ImageView image = new ImageView();
            image.setPreserveRatio(true);
            image.setSmooth(true);
            image.setFitWidth(imageWidth(height));
            image.setFitHeight(Math.max(1, height - 24));
            image.setVisible(false);
            image.setMouseTransparent(true);
            image.getStyleClass().add("product-image");
            StackPane.setAlignment(image, Pos.CENTER);
            StackPane.setMargin(image, new Insets(12));
            getChildren().add(image);

            IMAGE_SERVICE.load(imageUrl).thenAccept(result -> Platform.runLater(() -> {
                if (result.loaded()) {
                    image.setImage(result.image());
                    image.setVisible(true);
                    placeholder.setVisible(false);
                }
            }));
        }

        setAlignment(Pos.CENTER);
        setMinHeight(height);
        setPrefHeight(height);
        setMaxHeight(height);
        getStyleClass().addAll("product-artwork", artworkClass);
    }

    private static double imageWidth(double height) {
        return height >= 300 ? 380 : Math.max(96, height * 1.12);
    }

    private static double placeholderSize(double height) {
        return height <= 90 ? 26 : height >= 300 ? 72 : 54;
    }

}
