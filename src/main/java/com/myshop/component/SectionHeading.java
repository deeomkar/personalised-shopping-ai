package com.myshop.component;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class SectionHeading extends VBox {

    public SectionHeading(String title) {
        this(title, null);
    }

    public SectionHeading(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");
        getChildren().add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.getStyleClass().add("section-subtitle");
            getChildren().add(subtitleLabel);
        }

        getStyleClass().add("section-heading");
    }
}
