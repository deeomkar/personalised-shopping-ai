package com.myshop.view;

import com.myshop.model.User;
import com.myshop.model.UserPreference;
import com.myshop.service.HistoryService;
import com.myshop.service.PreferenceException;
import com.myshop.service.PreferenceService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class ProfileView extends javafx.scene.control.ScrollPane {
    private final User user;
    private final PreferenceService preferenceService;
    private final HistoryService historyService;
    private final UserPreference initial;
    private final Consumer<UserPreference> onSaved;
    private final Set<String> categories = new LinkedHashSet<>();
    private final Set<String> priorities = new LinkedHashSet<>();
    private final TextField brands = new TextField();
    private final Label status = new Label();
    private final javafx.scene.control.ToggleGroup styleGroup = new javafx.scene.control.ToggleGroup();

    public ProfileView(User user, UserPreference preference, PreferenceService preferenceService,
                       HistoryService historyService, Consumer<UserPreference> onSaved) {
        this.user = user; this.initial = preference; this.preferenceService = preferenceService; this.historyService = historyService; this.onSaved = onSaved;
        categories.addAll(preference.selectedCategories()); priorities.addAll(preference.shoppingPriorities()); brands.setText(preference.favoriteBrands());
        VBox page = new VBox(); page.setPadding(new Insets(44, 48, 72, 48)); page.setSpacing(24); page.getStyleClass().add("profile-page");
        Label title = new Label("Profile & settings"); title.getStyleClass().add("results-title");
        Label identity = new Label(user.name() + "  ·  " + user.email()); identity.getStyleClass().add("profile-identity");
        page.getChildren().addAll(title, identity, preferenceSection(), historySection());
        setContent(page); setFitToWidth(true); setHbarPolicy(ScrollBarPolicy.NEVER); setVbarPolicy(ScrollBarPolicy.AS_NEEDED); getStyleClass().add("screen-scroll");
    }

    private VBox preferenceSection() {
        VBox panel = new VBox(); panel.setSpacing(18); panel.getStyleClass().add("settings-panel");
        panel.getChildren().add(sectionLabel("Shopping preferences"));
        panel.getChildren().add(toggleGroup("Categories", PreferenceService.CATEGORY_OPTIONS, categories, false));
        panel.getChildren().add(toggleGroup("Priorities", PreferenceService.PRIORITY_OPTIONS, priorities, true));
        FlowPane styles = new FlowPane(); styles.setHgap(9); styles.setVgap(9);
        for (UserPreference.ShoppingStyle style : UserPreference.ShoppingStyle.values()) {
            ToggleButton button = new ToggleButton(style.label()); button.setToggleGroup(styleGroup); button.setSelected(style == initial.shoppingStyle()); button.getStyleClass().add("preference-style-toggle");
            button.setOnAction(event -> styles.getChildren().forEach(node -> { if (node != button) ((ToggleButton) node).setSelected(false); })); styles.getChildren().add(button);
        }
        panel.getChildren().addAll(sectionLabel("Shopping style"), styles);
        brands.setPromptText("Favourite brands (optional)"); brands.getStyleClass().add("onboarding-input");
        panel.getChildren().addAll(sectionLabel("Favourite brands"), brands);
        Button save = new Button("Save preferences"); save.getStyleClass().add("primary-button"); save.setOnAction(event -> save());
        status.getStyleClass().add("settings-status"); panel.getChildren().addAll(save, status);
        return panel;
    }

    private FlowPane toggleGroup(String title, java.util.List<String> options, Set<String> selected, boolean capped) {
        FlowPane group = new FlowPane(); group.setHgap(9); group.setVgap(9); group.getStyleClass().add("preference-toggle-grid");
        for (String option : options) {
            ToggleButton button = new ToggleButton(option); button.setSelected(selected.contains(option)); button.getStyleClass().add("preference-toggle");
            button.setOnAction(event -> { if (button.isSelected()) { if (capped && selected.size() >= PreferenceService.MAX_PRIORITIES) button.setSelected(false); else selected.add(option); } else selected.remove(option); }); group.getChildren().add(button);
        }
        return group;
    }

    private VBox historySection() {
        VBox panel = new VBox(); panel.setSpacing(12); panel.getStyleClass().add("settings-panel"); panel.getChildren().add(sectionLabel("Privacy controls"));
        Label copy = new Label("Clear recent searches or recently viewed products. Saved products are kept."); copy.setWrapText(true); copy.getStyleClass().add("detail-muted"); panel.getChildren().add(copy);
        HBox actions = new HBox(); actions.setSpacing(10);
        Button searches = new Button("Clear recent searches"); searches.getStyleClass().add("secondary-button"); searches.setOnAction(event -> confirm("Clear recent searches?", historyService::clearSearches));
        Button viewed = new Button("Clear recently viewed"); viewed.getStyleClass().add("secondary-button"); viewed.setOnAction(event -> confirm("Clear recently viewed?", historyService::clearViewed));
        actions.getChildren().addAll(searches, viewed); panel.getChildren().add(actions); return panel;
    }

    private Label sectionLabel(String text) { Label label = new Label(text); label.getStyleClass().add("detail-section-heading"); return label; }

    private void save() {
        UserPreference.ShoppingStyle style = styleGroup.getSelectedToggle() instanceof ToggleButton button
                ? UserPreference.ShoppingStyle.fromLabel(button.getText()) : UserPreference.ShoppingStyle.NO_PREFERENCE;
        try {
            UserPreference saved = preferenceService.save(new UserPreference(user.id(), categories, priorities, style, brands.getText(), true, java.time.Instant.now()));
            status.setText("Preferences saved"); onSaved.accept(saved);
        } catch (PreferenceException exception) { status.setText(exception.getMessage()); }
    }

    private void confirm(String message, java.util.function.LongConsumer action) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message); alert.setHeaderText(null); alert.showAndWait().filter(response -> response == javafx.scene.control.ButtonType.OK).ifPresent(ignored -> action.accept(user.id()));
    }
}
