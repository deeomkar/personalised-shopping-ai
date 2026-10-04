package com.myshop.view;

import com.myshop.component.CategoryCard;
import com.myshop.component.IconType;
import com.myshop.component.MyShopLogo;
import com.myshop.model.Category;
import com.myshop.model.User;
import com.myshop.model.UserPreference;
import com.myshop.service.PreferenceException;
import com.myshop.service.PreferenceService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class OnboardingView extends BorderPane {

    private static final int TOTAL_STEPS = 3;

    private final PreferenceService preferenceService;
    private final User user;
    private final Consumer<UserPreference> onFinished;
    private final VBox stepHost = new VBox();
    private final Label stepLabel = new Label();
    private final Label errorLabel = new Label();
    private final ProgressBar progressBar = new ProgressBar();
    private final Button backButton = new Button("Back");
    private final Button continueButton = new Button("Continue");
    private final Button finishButton = new Button("Finish");
    private final ProgressIndicator busyIndicator = new ProgressIndicator();
    private final Set<String> selectedCategories = new LinkedHashSet<>();
    private final Set<String> shoppingPriorities = new LinkedHashSet<>();
    private UserPreference.ShoppingStyle shoppingStyle = UserPreference.ShoppingStyle.NO_PREFERENCE;
    private final TextField favoriteBrands = new TextField();
    private int step = 1;
    private boolean busy;

    public OnboardingView(
            PreferenceService preferenceService,
            User user,
            Consumer<UserPreference> onFinished
    ) {
        this.preferenceService = Objects.requireNonNull(preferenceService, "preferenceService");
        this.user = Objects.requireNonNull(user, "user");
        this.onFinished = Objects.requireNonNull(onFinished, "onFinished");

        getStyleClass().add("onboarding-view");
        restoreExistingPreferences();
        setTop(createHeader());
        setCenter(createMainContent());
        renderStep();
    }

    private void restoreExistingPreferences() {
        preferenceService.load(user.id()).ifPresent(preferences -> {
            selectedCategories.addAll(preferences.selectedCategories());
            shoppingPriorities.addAll(preferences.shoppingPriorities());
            shoppingStyle = preferences.shoppingStyle();
            favoriteBrands.setText(preferences.favoriteBrands());
        });
    }

    private HBox createHeader() {
        Label brand = new Label("MyShop");
        brand.getStyleClass().add("onboarding-brand");
        HBox brandLockup = new HBox(new MyShopLogo(32), brand);
        brandLockup.setAlignment(Pos.CENTER_LEFT);
        brandLockup.setSpacing(10);

        Label userLabel = new Label("A quick setup for " + user.name());
        userLabel.getStyleClass().add("onboarding-user-label");

        HBox header = new HBox(brandLockup, userLabel);
        HBox.setHgrow(brandLockup, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(27, 42, 24, 42));
        header.getStyleClass().add("onboarding-header");
        return header;
    }

    private StackPane createMainContent() {
        Label kicker = new Label("Make MyShop feel more like yours");
        kicker.getStyleClass().add("onboarding-kicker");

        stepLabel.getStyleClass().add("onboarding-step-label");
        HBox stepMeta = new HBox(kicker, stepLabel);
        stepMeta.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(kicker, Priority.ALWAYS);

        progressBar.setMinHeight(5);
        progressBar.setPrefHeight(5);
        progressBar.setMaxHeight(5);
        progressBar.getStyleClass().add("onboarding-progress");

        stepHost.setSpacing(22);

        errorLabel.setWrapText(true);
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.getStyleClass().add("onboarding-error");

        busyIndicator.setPrefSize(16, 16);
        busyIndicator.setMinSize(16, 16);
        busyIndicator.setMaxSize(16, 16);
        busyIndicator.setManaged(false);
        busyIndicator.setVisible(false);

        backButton.setMnemonicParsing(false);
        backButton.getStyleClass().add("secondary-button");
        backButton.setOnAction(event -> goBack());

        continueButton.setMnemonicParsing(false);
        continueButton.getStyleClass().add("primary-button");
        continueButton.setOnAction(event -> continueToNextStep());

        finishButton.setMnemonicParsing(false);
        finishButton.getStyleClass().add("primary-button");
        finishButton.setOnAction(event -> finish());

        HBox actions = new HBox(backButton, continueButton, finishButton, busyIndicator);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setSpacing(10);

        VBox card = new VBox(stepMeta, progressBar, stepHost, errorLabel, actions);
        card.setSpacing(24);
        card.setMaxWidth(760);
        card.setPadding(new Insets(38, 42, 34, 42));
        card.getStyleClass().add("onboarding-card");

        StackPane area = new StackPane(card);
        area.setPadding(new Insets(22, 32, 42, 32));
        area.getStyleClass().add("onboarding-content");
        return area;
    }

    private void renderStep() {
        clearError();
        stepLabel.setText(step + " of " + TOTAL_STEPS);
        progressBar.setProgress((double) step / TOTAL_STEPS);
        backButton.setDisable(step == 1 || busy);
        continueButton.setVisible(step < TOTAL_STEPS);
        continueButton.setManaged(step < TOTAL_STEPS);
        finishButton.setVisible(step == TOTAL_STEPS);
        finishButton.setManaged(step == TOTAL_STEPS);

        stepHost.getChildren().setAll(switch (step) {
            case 1 -> createCategoriesStep();
            case 2 -> createPrioritiesStep();
            case 3 -> createStyleStep();
            default -> throw new IllegalStateException("Unknown onboarding step: " + step);
        });
    }

    private VBox createCategoriesStep() {
        VBox content = stepContent(
                "What do you usually shop for?",
                "Choose the categories you care about most."
        );

        FlowPane choices = new FlowPane();
        choices.setHgap(12);
        choices.setVgap(12);
        choices.getStyleClass().add("preference-grid");

        for (Category category : categoryOptions()) {
            CategoryCard card = new CategoryCard(category);
            card.setAccessibleText("Select " + category.name());
            if (selectedCategories.contains(category.name())) {
                card.getStyleClass().add("preference-selected");
            }
            card.setOnAction(event -> {
                if (selectedCategories.contains(category.name())) {
                    selectedCategories.remove(category.name());
                    card.getStyleClass().remove("preference-selected");
                } else {
                    selectedCategories.add(category.name());
                    card.getStyleClass().add("preference-selected");
                }
            });
            choices.getChildren().add(card);
        }
        content.getChildren().add(choices);
        return content;
    }

    private VBox createPrioritiesStep() {
        VBox content = stepContent(
                "What matters most when you shop?",
                "Pick the things you usually care about first."
        );

        FlowPane choices = new FlowPane();
        choices.setHgap(10);
        choices.setVgap(10);
        choices.getStyleClass().add("preference-toggle-grid");

        for (String priority : PreferenceService.PRIORITY_OPTIONS) {
            ToggleButton choice = new ToggleButton(priority);
            choice.setMnemonicParsing(false);
            choice.setUserData(priority);
            choice.setSelected(shoppingPriorities.contains(priority));
            choice.getStyleClass().add("preference-toggle");
            choice.setOnAction(event -> {
                if (choice.isSelected()) {
                    if (shoppingPriorities.size() >= PreferenceService.MAX_PRIORITIES) {
                        choice.setSelected(false);
                        showError("Choose up to 4 priorities.");
                        return;
                    }
                    shoppingPriorities.add(priority);
                } else {
                    shoppingPriorities.remove(priority);
                }
                clearError();
            });
            choices.getChildren().add(choice);
        }

        Label hint = new Label("Select up to 4");
        hint.getStyleClass().add("onboarding-hint");
        content.getChildren().addAll(choices, hint);
        return content;
    }

    private VBox createStyleStep() {
        VBox content = stepContent(
                "How do you usually shop?",
                "Choose the approach that feels most like you."
        );

        ToggleGroup styleGroup = new ToggleGroup();
        FlowPane styles = new FlowPane();
        styles.setHgap(10);
        styles.setVgap(10);
        styles.getStyleClass().add("preference-style-grid");

        for (UserPreference.ShoppingStyle style : UserPreference.ShoppingStyle.values()) {
            ToggleButton choice = new ToggleButton(style.label());
            choice.setMnemonicParsing(false);
            choice.setToggleGroup(styleGroup);
            choice.setUserData(style);
            choice.setSelected(style == shoppingStyle);
            choice.getStyleClass().add("preference-style-toggle");
            choice.setOnAction(event -> shoppingStyle = style);
            styles.getChildren().add(choice);
        }

        Label brandsLabel = new Label("Favourite brands");
        brandsLabel.getStyleClass().add("onboarding-field-label");
        favoriteBrands.setPromptText("Nike, Sony, Adidas");
        favoriteBrands.setAccessibleText("Favourite brands, optional");
        favoriteBrands.getStyleClass().add("onboarding-input");
        favoriteBrands.setMaxWidth(Double.MAX_VALUE);

        VBox brandField = new VBox(brandsLabel, favoriteBrands);
        brandField.setSpacing(7);
        brandField.getStyleClass().add("onboarding-field");
        content.getChildren().addAll(styles, brandField);
        return content;
    }

    private VBox stepContent(String titleText, String supportingText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("onboarding-title");
        Label supporting = new Label(supportingText);
        supporting.setWrapText(true);
        supporting.getStyleClass().add("onboarding-supporting");

        VBox content = new VBox(title, supporting);
        content.setSpacing(7);
        content.getStyleClass().add("onboarding-step-content");
        return content;
    }

    private void goBack() {
        if (busy || step == 1) {
            return;
        }
        step--;
        renderStep();
    }

    private void continueToNextStep() {
        clearError();
        try {
            if (step == 1) {
                preferenceService.validateCategories(selectedCategories);
            } else if (step == 2) {
                preferenceService.validatePriorities(shoppingPriorities);
            }
            step++;
            renderStep();
        } catch (PreferenceException exception) {
            showError(exception.getMessage());
        }
    }

    private void finish() {
        clearError();
        try {
            preferenceService.validateCategories(selectedCategories);
            preferenceService.validatePriorities(shoppingPriorities);
        } catch (PreferenceException exception) {
            showError(exception.getMessage());
            return;
        }

        Set<String> categories = new LinkedHashSet<>(selectedCategories);
        Set<String> priorities = new LinkedHashSet<>(shoppingPriorities);
        String brands = favoriteBrands.getText();
        setBusy(true);

        Task<UserPreference> task = new Task<>() {
            @Override
            protected UserPreference call() {
                return preferenceService.completeOnboarding(
                        user.id(), categories, priorities, shoppingStyle, brands
                );
            }
        };
        task.setOnSucceeded(event -> {
            setBusy(false);
            onFinished.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            setBusy(false);
            Throwable exception = task.getException();
            showError(exception instanceof PreferenceException
                    ? exception.getMessage()
                    : "We couldn't save your preferences. Please try again.");
        });

        Thread thread = new Thread(task, "myshop-preferences-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void setBusy(boolean value) {
        busy = value;
        backButton.setDisable(value || step == 1);
        continueButton.setDisable(value);
        finishButton.setDisable(value);
        busyIndicator.setManaged(value);
        busyIndicator.setVisible(value);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private Category[] categoryOptions() {
        Map<String, Category> categories = new LinkedHashMap<>();
        categories.put("Fashion", new Category("Fashion", IconType.SHIRT, "category-sage"));
        categories.put("Footwear", new Category("Footwear", IconType.SHOE, "category-sand"));
        categories.put("Electronics", new Category("Electronics", IconType.HEADPHONES, "category-blue"));
        categories.put("Beauty", new Category("Beauty", IconType.BEAUTY, "category-blush"));
        categories.put("Accessories", new Category("Accessories", IconType.WATCH, "category-lilac"));
        categories.put("Home", new Category("Home", IconType.HOME, "category-clay"));
        categories.put("Sports", new Category("Sports", IconType.SPORTS, "category-sky"));
        return categories.values().toArray(Category[]::new);
    }
}
