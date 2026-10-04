package com.myshop.component;

import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.HBox;

public final class SearchField extends HBox {

    private final TextField input;

    public SearchField(String prompt) {
        IconView icon = new IconView(IconType.SEARCH, 19);
        icon.setStrokeColor(javafx.scene.paint.Color.web("#7E817D"));

        input = new TextField();
        input.setPromptText(prompt);
        input.setAccessibleText(prompt);
        input.setFocusTraversable(true);
        input.getStyleClass().add("search-input");

        getChildren().addAll(icon, input);
        setSpacing(10);
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
