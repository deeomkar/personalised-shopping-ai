package com.myshop.component;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public final class PriceView extends HBox {

    public PriceView(String price, String originalPrice, String discount) {
        if (price != null && !price.isBlank()) {
            Label current = new Label(price);
            current.getStyleClass().add("product-price");
            getChildren().add(current);
        }

        if (originalPrice != null && !originalPrice.isBlank()) {
            Label original = new Label(originalPrice);
            original.getStyleClass().add("original-price");
            getChildren().add(original);
        }

        if (discount != null && !discount.isBlank()) {
            Label discountLabel = new Label(discount);
            discountLabel.getStyleClass().add("discount-pill");
            getChildren().add(discountLabel);
        }

        setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        setSpacing(8);
        getStyleClass().add("price-row");
    }
}
