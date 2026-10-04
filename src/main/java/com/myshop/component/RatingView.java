package com.myshop.component;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public final class RatingView extends HBox {

    public RatingView(double rating, int reviewCount) {
        IconView star = new IconView(IconType.STAR, 13);
        star.setStrokeColor(Color.web("#B27A37"));

        Label ratingLabel = new Label(String.format("%.1f", rating));
        ratingLabel.getStyleClass().add("rating-value");

        Label reviews = new Label("(" + reviewCount + ")");
        reviews.getStyleClass().add("rating-reviews");

        getChildren().addAll(star, ratingLabel, reviews);
        setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        setSpacing(4);
        getStyleClass().add("rating-row");
    }
}
