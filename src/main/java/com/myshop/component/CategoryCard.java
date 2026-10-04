package com.myshop.component;

import com.myshop.model.Category;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Objects;
import java.util.function.Consumer;

public final class CategoryCard extends Button {

    public CategoryCard(Category category) {
        this(category, ignored -> {
        });
    }

    public CategoryCard(Category category, Consumer<Category> onSelected) {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(onSelected, "onSelected");
        StackPane iconTile = new StackPane(new IconView(category.icon(), 24));
        iconTile.getStyleClass().addAll("category-icon-tile", category.tintClass());

        Label name = new Label(category.name());
        name.getStyleClass().add("category-name");

        VBox content = new VBox(iconTile, name);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setSpacing(15);

        setGraphic(content);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setMnemonicParsing(false);
        setAccessibleText("Browse " + category.name());
        setOnAction(event -> onSelected.accept(category));
        setMinSize(148, 118);
        setPrefSize(148, 118);
        setMaxSize(148, 118);
        getStyleClass().add("category-card");

        Color iconColor = Color.web("#315A48");
        ((IconView) iconTile.getChildren().getFirst()).setStrokeColor(iconColor);
    }
}
