package com.myshop.component;

import com.myshop.model.Product;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
        IconView placeholder = new IconView(iconType == null ? IconType.BAG : iconType, placeholderSize(height));
        placeholder.setStrokeColor(Color.web("#F8F6F0"));
        placeholder.getStyleClass().add("product-image-placeholder");
        getChildren().add(placeholder);

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                Image remoteImage = new Image(imageUrl.trim(), true);
                ImageView image = new ImageView(remoteImage);
                image.setPreserveRatio(true);
                image.setSmooth(true);
                image.setFitWidth(imageWidth(height));
                image.setFitHeight(Math.max(1, height - 24));
                image.setVisible(false);
                image.getStyleClass().add("product-image");
                StackPane.setAlignment(image, Pos.CENTER);
                StackPane.setMargin(image, new Insets(12));
                getChildren().add(image);

                Runnable updateImageState = () -> {
                    boolean ready = !remoteImage.isError()
                            && remoteImage.getProgress() >= 1.0
                            && remoteImage.getWidth() > 0;
                    image.setVisible(ready);
                    placeholder.setVisible(!ready);
                };
                remoteImage.progressProperty().addListener((observable, oldValue, newValue) -> updateImageState.run());
                remoteImage.errorProperty().addListener((observable, oldValue, newValue) -> updateImageState.run());
                updateImageState.run();
            } catch (IllegalArgumentException ignored) {
                // Keep the neutral artwork when a provider supplies an invalid image URL.
            }
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
