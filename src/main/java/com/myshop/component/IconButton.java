package com.myshop.component;

import javafx.scene.control.Button;
import javafx.scene.paint.Color;

public final class IconButton extends Button {

    private final IconView icon;

    public IconButton(IconType type, String accessibleText) {
        icon = new IconView(type, 18);
        setGraphic(icon);
        setMnemonicParsing(false);
        setAccessibleText(accessibleText);
        getStyleClass().add("icon-button");
    }

    public void setIconColor(Color color) {
        icon.setStrokeColor(color);
    }
}
