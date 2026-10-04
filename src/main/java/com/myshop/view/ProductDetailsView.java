package com.myshop.view;

import com.myshop.component.IconType;
import com.myshop.component.IconView;
import com.myshop.component.PriceView;
import com.myshop.component.ProductArtwork;
import com.myshop.component.RatingView;
import com.myshop.component.RecommendationNote;
import com.myshop.model.Product;
import com.myshop.model.ProductOffer;
import com.myshop.model.Recommendation;
import com.myshop.service.ExternalLinkService;
import com.myshop.service.ProductOfferService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class ProductDetailsView extends ScrollPane {
    public ProductDetailsView(Product product, Runnable backAction) {
        this(product, null, backAction, null, () -> product.saved(), null, null, null, new ProductOfferService());
    }

    public ProductDetailsView(Product product, Recommendation recommendation, Runnable backAction,
                              Runnable saveAction, BooleanSupplier saved, Runnable compareAction,
                              Runnable viewedAction, Runnable refreshSavedAction) {
        this(product, recommendation, backAction, saveAction, saved, compareAction, viewedAction,
                refreshSavedAction, new ProductOfferService());
    }

    public ProductDetailsView(Product product, Recommendation recommendation, Runnable backAction,
                              Runnable saveAction, BooleanSupplier saved, Runnable compareAction,
                              Runnable viewedAction, Runnable refreshSavedAction,
                              ProductOfferService offerService) {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(backAction, "backAction");
        if (viewedAction != null) viewedAction.run();

        Button back = new Button("Back");
        back.setMnemonicParsing(false);
        IconView arrow = new IconView(IconType.ARROW_RIGHT, 16); arrow.setRotate(180); back.setGraphic(arrow);
        back.setOnAction(event -> backAction.run()); back.getStyleClass().add("secondary-button");

        ProductArtwork artwork = new ProductArtwork(product, 340);
        artwork.setMinWidth(410); artwork.setPrefWidth(410); artwork.setMaxWidth(510);
        artwork.getStyleClass().add("detail-artwork");

        VBox copy = new VBox(); copy.setSpacing(12); copy.setMaxWidth(540); copy.getStyleClass().add("detail-copy");
        addText(copy, product.brand(), "detail-brand", true);
        addText(copy, product.category(), "detail-category", false);
        addText(copy, product.name(), "detail-name", true);
        if (hasRating(product)) copy.getChildren().add(new RatingView(product.rating(), product.reviewCount()));
        if (hasText(product.price())) copy.getChildren().add(new PriceView(product.price(), product.originalPrice(), product.discount()));
        if (recommendation != null && !recommendation.reasons().isEmpty()) copy.getChildren().add(new RecommendationNote(recommendation));

        HBox actions = new HBox(); actions.setSpacing(10);
        if (saveAction != null) {
            Button save = new Button(); save.setMnemonicParsing(false); save.getStyleClass().add("secondary-button");
            Runnable update = () -> save.setText(saved != null && saved.getAsBoolean() ? "♥ Saved" : "♡ Save");
            update.run();
            save.setOnAction(event -> { saveAction.run(); update.run(); if (refreshSavedAction != null) refreshSavedAction.run(); });
            actions.getChildren().add(save);
        }
        if (compareAction != null) {
            Button compare = new Button("Compare"); compare.setMnemonicParsing(false); compare.getStyleClass().add("secondary-button");
            compare.setOnAction(event -> compareAction.run()); actions.getChildren().add(compare);
        }
        if (!actions.getChildren().isEmpty()) copy.getChildren().add(actions);

        HBox productPanel = new HBox(artwork, copy); productPanel.setAlignment(Pos.TOP_LEFT); productPanel.setSpacing(46);
        productPanel.getStyleClass().add("detail-panel"); HBox.setHgrow(copy, Priority.ALWAYS);

        VBox lower = new VBox(); lower.setSpacing(22);
        if (hasText(product.description())) {
            Label heading = new Label("About this product"); heading.getStyleClass().add("detail-section-heading");
            Label description = new Label(product.description()); description.setWrapText(true); description.getStyleClass().add("detail-description");
            lower.getChildren().add(new VBox(heading, description));
        }
        lower.getChildren().add(createOffers(product, offerService));

        VBox content = new VBox(back, productPanel, lower); content.setPadding(new Insets(44, 48, 72, 48));
        content.setSpacing(28); content.getStyleClass().add("details-content");
        setContent(content); setFitToWidth(true); setHbarPolicy(ScrollBarPolicy.NEVER); setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        getStyleClass().add("screen-scroll");
    }

    private VBox createOffers(Product product, ProductOfferService offerService) {
        Label heading = new Label("Store offers"); heading.getStyleClass().add("detail-section-heading");
        VBox panel = new VBox(heading); panel.setSpacing(10); panel.getStyleClass().add("offers-panel");
        var offers = offerService.offersFor(product);
        if (offers.isEmpty()) {
            Label empty = new Label("Store comparison isn’t available for this product."); empty.getStyleClass().add("detail-muted");
            panel.getChildren().add(empty); return panel;
        }
        offers.forEach(offer -> {
            HBox row = new HBox(); row.setAlignment(Pos.CENTER_LEFT); row.setSpacing(14); row.getStyleClass().add("offer-row");
            Label store = new Label(offer.storeName()); store.getStyleClass().add("offer-store");
            Label price = new Label(offer.price().toPlainString() + " " + offer.currency()); price.getStyleClass().add("offer-price");
            VBox meta = new VBox(store, price); meta.setSpacing(3); HBox.setHgrow(meta, Priority.ALWAYS); row.getChildren().add(meta);
            if (offer.delivery() != null) { Label delivery = new Label(offer.delivery()); delivery.getStyleClass().add("detail-muted"); row.getChildren().add(delivery); }
            if (ExternalLinkService.isSafe(offer.productUrl())) {
                Button view = new Button("View product"); view.setMnemonicParsing(false); view.getStyleClass().add("secondary-button");
                view.setOnAction(event -> { if (ExternalLinkService.open(offer.productUrl()) != ExternalLinkService.Result.OPENED) view.setText("Link unavailable"); });
                row.getChildren().add(view);
            }
            panel.getChildren().add(row);
        });
        return panel;
    }

    private void addText(VBox target, String value, String style, boolean required) {
        if (!hasText(value)) return;
        Label label = new Label(required && "detail-brand".equals(style) ? value.toUpperCase() : value);
        label.setWrapText(true); label.getStyleClass().add(style); target.getChildren().add(label);
    }

    private boolean hasRating(Product product) { return Double.isFinite(product.rating()) && product.rating() > 0; }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
