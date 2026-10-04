package com.myshop.component;

import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;

/**
 * Small, reusable MyShop brand mark: a shopping bag with an integrated M.
 */
public final class MyShopLogo extends StackPane {

    private static final double VIEWBOX_SIZE = 32;
    private static final Color FOREST = Color.web("#1F4D3B");
    private static final Color SAGE = Color.web("#E8F0EB");

    public MyShopLogo() {
        this(32);
    }

    public MyShopLogo(double size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Logo size must be positive");
        }

        getStyleClass().add("myshop-logo");
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
        setMouseTransparent(true);
        setFocusTraversable(false);

        Group vector = new Group(createHandle(), createBag(), createLetterM());
        double scale = size / VIEWBOX_SIZE;
        vector.setScaleX(scale);
        vector.setScaleY(scale);
        getChildren().add(vector);
    }

    private Path createHandle() {
        Path handle = new Path(
                new MoveTo(10, 10),
                new CubicCurveTo(10, 4.5, 22, 4.5, 22, 10)
        );
        handle.setFill(null);
        handle.setStroke(FOREST);
        handle.setStrokeWidth(1.8);
        handle.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        return handle;
    }

    private Rectangle createBag() {
        Rectangle bag = new Rectangle(4, 9, 24, 19);
        bag.setArcWidth(5);
        bag.setArcHeight(5);
        bag.setFill(SAGE);
        bag.setStroke(FOREST);
        bag.setStrokeWidth(1.6);
        return bag;
    }

    private Path createLetterM() {
        Path letterM = new Path(
                new MoveTo(9, 23),
                new LineTo(9, 15),
                new LineTo(16, 21),
                new LineTo(23, 15),
                new LineTo(23, 23)
        );
        letterM.setFill(null);
        letterM.setStroke(FOREST);
        letterM.setStrokeWidth(2.1);
        letterM.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        letterM.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        return letterM;
    }
}
