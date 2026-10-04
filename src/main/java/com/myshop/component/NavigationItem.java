package com.myshop.component;

import com.myshop.navigation.NavigationDestination;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public final class NavigationItem extends Button {

    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");

    private final NavigationDestination destination;
    private final IconView icon;

    public NavigationItem(NavigationDestination destination) {
        this.destination = destination;
        this.icon = new IconView(destination.icon(), 17);

        setText(destination.label());
        setGraphic(icon);
        setGraphicTextGap(9);
        setMnemonicParsing(false);
        getStyleClass().add("nav-item");
        setAccessibleText(destination.label() + " navigation");
    }

    public NavigationDestination destination() {
        return destination;
    }

    public void setActive(boolean active) {
        pseudoClassStateChanged(ACTIVE, active);
        icon.setStrokeColor(active
                ? javafx.scene.paint.Color.web("#1F4D3B")
                : javafx.scene.paint.Color.web("#737373"));
    }
}
