package com.myshop.component;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.List;

public final class IconView extends StackPane {

    private static final double ICON_GRID = 24;
    private final List<Shape> strokes = new ArrayList<>();
    private final double size;
    private Color strokeColor = Color.web("#737373");

    public IconView(IconType type, double size) {
        this.size = size;
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
        setMouseTransparent(true);

        Group artwork = new Group(createArtwork(type));
        artwork.setScaleX(size / ICON_GRID);
        artwork.setScaleY(size / ICON_GRID);
        getChildren().add(artwork);
        setStrokeColor(strokeColor);
    }

    public void setStrokeColor(Color color) {
        strokeColor = color;
        strokes.forEach(shape -> shape.setStroke(color));
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    private Node createArtwork(IconType type) {
        return switch (type) {
            case SEARCH -> search();
            case HEART -> heart();
            case USER -> user();
            case CHEVRON_DOWN -> chevronDown();
            case DISCOVER -> discover();
            case SAVED -> saved();
            case COMPARE -> compare();
            case ARROW_RIGHT -> arrowRight();
            case STAR -> star();
            case SHIRT -> shirt();
            case SHOE -> shoe();
            case HEADPHONES -> headphones();
            case BEAUTY -> beauty();
            case WATCH -> watch();
            case HOME -> home();
            case SPORTS -> sports();
            case BAG -> bag();
        };
    }

    private Group search() {
        Circle circle = circle(10.2, 10.2, 5.7);
        Line handle = line(14.5, 14.5, 19.2, 19.2);
        return group(circle, handle);
    }

    private Group heart() {
        Path path = new Path(
                move(12, 19.4),
                curve(10.6, 18.1, 4.2, 14.3, 4.2, 9.1),
                curve(4.2, 6.4, 6.0, 4.6, 8.3, 4.6),
                curve(10.0, 4.6, 11.3, 5.5, 12, 6.8),
                curve(12.7, 5.5, 14.0, 4.6, 15.7, 4.6),
                curve(18.0, 4.6, 19.8, 6.4, 19.8, 9.1),
                curve(19.8, 14.3, 13.4, 18.1, 12, 19.4),
                new ClosePath()
        );
        path.setFill(Color.TRANSPARENT);
        addStroke(path);
        return group(path);
    }

    private Group user() {
        Circle head = circle(12, 8.3, 3.1);
        Arc shoulders = new Arc(12, 20, 7.6, 6.1, 20, 140);
        shoulders.setType(ArcType.OPEN);
        return group(head, shoulders);
    }

    private Group chevronDown() {
        return group(line(7.2, 9.2, 12, 14), line(12, 14, 16.8, 9.2));
    }

    private Group discover() {
        Circle outer = circle(12, 12, 8.2);
        Circle inner = circle(12, 12, 2.4);
        Line rayOne = line(12, 3.8, 12, 6);
        Line rayTwo = line(12, 18, 12, 20.2);
        Line rayThree = line(3.8, 12, 6, 12);
        Line rayFour = line(18, 12, 20.2, 12);
        return group(outer, inner, rayOne, rayTwo, rayThree, rayFour);
    }

    private Group saved() {
        Path path = new Path(
                move(6.3, 4.5),
                lineTo(17.7, 4.5),
                lineTo(17.7, 19.4),
                lineTo(12, 15.2),
                lineTo(6.3, 19.4),
                lineTo(6.3, 4.5),
                new ClosePath()
        );
        path.setFill(Color.TRANSPARENT);
        addStroke(path);
        return group(path);
    }

    private Group compare() {
        Rectangle left = rectangle(5.2, 6.4, 5.1, 11.2, 1.2);
        Rectangle right = rectangle(13.7, 6.4, 5.1, 11.2, 1.2);
        Line divider = line(12, 4.1, 12, 19.9);
        return group(left, right, divider);
    }

    private Group arrowRight() {
        return group(line(4.8, 12, 19, 12), line(13.8, 6.8, 19, 12), line(13.8, 17.2, 19, 12));
    }

    private Group star() {
        Polygon star = new Polygon();
        for (int point = 0; point < 10; point++) {
            double angle = -Math.PI / 2 + point * Math.PI / 5;
            double radius = point % 2 == 0 ? 8.3 : 3.6;
            star.getPoints().addAll(12 + Math.cos(angle) * radius, 12 + Math.sin(angle) * radius);
        }
        star.setFill(Color.TRANSPARENT);
        addStroke(star);
        return group(star);
    }

    private Group shirt() {
        Path path = new Path(
                move(8.4, 5.1),
                lineTo(4.3, 8.1),
                lineTo(7.1, 12.0),
                lineTo(8.9, 10.7),
                lineTo(8.9, 19.4),
                lineTo(15.1, 19.4),
                lineTo(15.1, 10.7),
                lineTo(16.9, 12.0),
                lineTo(19.7, 8.1),
                lineTo(15.6, 5.1),
                curve(14.6, 7.1, 9.4, 7.1, 8.4, 5.1),
                new ClosePath()
        );
        path.setFill(Color.TRANSPARENT);
        addStroke(path);
        return group(path);
    }

    private Group shoe() {
        Path path = new Path(
                move(4.3, 14.3),
                curve(7.4, 14.2, 8.4, 12.6, 9.4, 9.8),
                curve(10.8, 11.6, 12.0, 14.1, 15.3, 15.4),
                curve(17.0, 16.1, 19.4, 16.7, 19.8, 18.3),
                curve(19.9, 19.0, 19.2, 19.5, 18.1, 19.5),
                lineTo(5.3, 19.5),
                curve(4.3, 19.5, 3.8, 18.8, 4.3, 18.0),
                lineTo(5.4, 16.3),
                curve(5.5, 15.5, 5.1, 14.9, 4.3, 14.3),
                new ClosePath()
        );
        path.setFill(Color.TRANSPARENT);
        addStroke(path);
        return group(path);
    }

    private Group headphones() {
        Arc band = new Arc(12, 12.5, 7.2, 7.2, 25, 130);
        band.setType(ArcType.OPEN);
        Rectangle left = rectangle(4.5, 13, 3, 5, 1.1);
        Rectangle right = rectangle(16.5, 13, 3, 5, 1.1);
        return group(band, left, right);
    }

    private Group beauty() {
        Rectangle bottle = rectangle(8, 9, 8, 10.2, 1.8);
        Rectangle cap = rectangle(9.5, 5.1, 5, 3.9, 1.2);
        Line shoulder = line(8, 9.5, 16, 9.5);
        return group(bottle, cap, shoulder);
    }

    private Group watch() {
        Rectangle strapTop = rectangle(10.2, 3.5, 3.6, 5, 1.5);
        Rectangle strapBottom = rectangle(10.2, 15.5, 3.6, 5, 1.5);
        Circle face = circle(12, 12, 5.4);
        Line handOne = line(12, 12, 12, 8.9);
        Line handTwo = line(12, 12, 14.4, 13.5);
        return group(strapTop, strapBottom, face, handOne, handTwo);
    }

    private Group home() {
        Path roof = new Path(
                move(4.4, 11.0), lineTo(12, 4.5), lineTo(19.6, 11.0), new ClosePath()
        );
        roof.setFill(Color.TRANSPARENT);
        addStroke(roof);
        Rectangle body = rectangle(6.3, 10.1, 11.4, 9, 0.7);
        Rectangle door = rectangle(10.4, 14.3, 3.2, 4.8, 0.4);
        return group(roof, body, door);
    }

    private Group sports() {
        Circle ball = circle(12, 12, 7.5);
        Arc seam = new Arc(12, 12, 7.5, 7.5, 70, 140);
        seam.setType(ArcType.OPEN);
        Line seamTwo = line(7.0, 6.5, 17.0, 17.5);
        return group(ball, seam, seamTwo);
    }

    private Group bag() {
        Rectangle body = rectangle(5.2, 8.2, 13.6, 11.2, 1.2);
        Arc handle = new Arc(12, 9, 4, 4, 180, 180);
        handle.setType(ArcType.OPEN);
        return group(body, handle);
    }

    private Group group(Shape... shapes) {
        return new Group(shapes);
    }

    private Circle circle(double centerX, double centerY, double radius) {
        Circle circle = new Circle(centerX, centerY, radius);
        addStroke(circle);
        return circle;
    }

    private Line line(double startX, double startY, double endX, double endY) {
        Line line = new Line(startX, startY, endX, endY);
        addStroke(line);
        return line;
    }

    private Rectangle rectangle(double x, double y, double width, double height, double arc) {
        Rectangle rectangle = new Rectangle(x, y, width, height);
        rectangle.setArcWidth(arc);
        rectangle.setArcHeight(arc);
        addStroke(rectangle);
        return rectangle;
    }

    private void addStroke(Shape shape) {
        shape.setFill(Color.TRANSPARENT);
        shape.setStroke(strokeColor);
        shape.setStrokeWidth(1.6);
        shape.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        shape.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        strokes.add(shape);
    }

    private javafx.scene.shape.MoveTo move(double x, double y) {
        return new javafx.scene.shape.MoveTo(x, y);
    }

    private javafx.scene.shape.LineTo lineTo(double x, double y) {
        return new javafx.scene.shape.LineTo(x, y);
    }

    private javafx.scene.shape.CubicCurveTo curve(double controlX1, double controlY1,
                                                   double controlX2, double controlY2,
                                                   double x, double y) {
        return new javafx.scene.shape.CubicCurveTo(controlX1, controlY1, controlX2, controlY2, x, y);
    }
}
