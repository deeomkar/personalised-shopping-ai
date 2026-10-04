package com.myshop.component;

import com.myshop.model.Recommendation;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Objects;

/** A restrained, human-readable explanation for a ranked product. */
public final class RecommendationNote extends VBox {

    public RecommendationNote(Recommendation recommendation) {
        Objects.requireNonNull(recommendation, "recommendation");

        Label match = new Label(recommendation.matchLabel());
        match.getStyleClass().add("recommendation-label");

        Label title = new Label("Why this matches");
        title.getStyleClass().add("recommendation-title");

        VBox reasons = new VBox();
        reasons.setSpacing(2);
        for (var reason : recommendation.reasons()) {
            Label reasonLabel = new Label("• " + reason.text());
            reasonLabel.setWrapText(true);
            reasonLabel.getStyleClass().add("recommendation-reason");
            reasons.getChildren().add(reasonLabel);
        }

        getChildren().addAll(match, title, reasons);
        setSpacing(3);
        getStyleClass().add("recommendation-note");
    }
}
