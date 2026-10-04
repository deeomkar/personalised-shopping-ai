package com.myshop.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public final class EmptyState extends VBox {

    public EmptyState(IconType iconType, String title, String message, String actionText, Runnable action) {
        IconView icon = new IconView(iconType, 28);
        icon.setStrokeColor(Color.web("#1F4D3B"));

        VBox iconTile = new VBox(icon);
        iconTile.setAlignment(Pos.CENTER);
        iconTile.getStyleClass().add("empty-icon-tile");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-title");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("empty-message");

        Button actionButton = new Button(actionText);
        actionButton.setMnemonicParsing(false);
        actionButton.setOnAction(event -> action.run());
        actionButton.getStyleClass().add("secondary-button");

        getChildren().addAll(iconTile, titleLabel, messageLabel, actionButton);
        setAlignment(Pos.CENTER);
        setSpacing(14);
        setMaxWidth(420);
        getStyleClass().add("empty-state");
    }
}
