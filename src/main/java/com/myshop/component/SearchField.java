package com.myshop.component;

import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class SearchField extends HBox {

    private final TextField input;

    public SearchField(String prompt) {
        IconView icon = new IconView(IconType.SEARCH, 19);
        icon.setStrokeColor(javafx.scene.paint.Color.web("#7E817D"));

        input = new TextField();
        input.setPromptText(prompt);
        input.setAccessibleText(prompt);
        input.setEditable(true);
        input.setDisable(false);
        input.setFocusTraversable(true);
        input.setMaxWidth(Double.MAX_VALUE);
        input.getStyleClass().add("search-input");

        icon.setMouseTransparent(true);
        getChildren().addAll(icon, input);
        HBox.setHgrow(input, Priority.ALWAYS);
        setSpacing(10);
        setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        setPickOnBounds(true);
        setOnMousePressed(event -> input.requestFocus());
        getStyleClass().add("search-field");
    }

    public TextField input() {
        return input;
    }

    public void setOnAction(EventHandler<ActionEvent> handler) {
        input.setOnAction(handler);
    }

    public void setInvalid(boolean invalid) {
        if (invalid && !getStyleClass().contains("search-invalid")) {
            getStyleClass().add("search-invalid");
        } else if (!invalid) {
            getStyleClass().remove("search-invalid");
        }
    }
}
