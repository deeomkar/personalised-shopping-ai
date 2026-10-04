package com.myshop.view;

import com.myshop.controller.AuthController;
import com.myshop.component.MyShopLogo;
import com.myshop.model.User;
import com.myshop.service.AuthException;
import com.myshop.service.AuthService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.function.Consumer;

public final class AuthView extends BorderPane {

    private enum Mode {
        LOGIN,
        REGISTER
    }

    private final AuthController controller;
    private final Consumer<User> onAuthenticated;
    private final VBox formHost = new VBox();
    private final Button loginTab = new Button("Log in");
    private final Button registerTab = new Button("Create account");
    private final ProgressIndicator progress = new ProgressIndicator();
    private final Label status = new Label();

    private Mode mode = Mode.LOGIN;
    private boolean busy;

    public AuthView(AuthController controller, Consumer<User> onAuthenticated) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.onAuthenticated = Objects.requireNonNull(onAuthenticated, "onAuthenticated");
        getStyleClass().add("auth-view");
        setLeft(createBrandPanel());
        setCenter(createAuthPanel());
        switchMode(Mode.LOGIN);
    }

    private VBox createBrandPanel() {
        Label brand = new Label("MyShop");
        brand.getStyleClass().add("auth-brand");

        Label accent = new Label();
        accent.getStyleClass().add("auth-brand-accent");

        Label headline = new Label("Shopping that starts with you.");
        headline.setWrapText(true);
        headline.getStyleClass().add("auth-headline");

        Label supporting = new Label("Find products that fit your preferences, budget and everyday needs.");
        supporting.setWrapText(true);
        supporting.getStyleClass().add("auth-supporting");

        HBox brandLockup = new HBox(new MyShopLogo(42), brand);
        brandLockup.setAlignment(Pos.CENTER_LEFT);
        brandLockup.setSpacing(12);
        brandLockup.getStyleClass().add("auth-brand-lockup");

        VBox content = new VBox(brandLockup, accent, headline, supporting);
        content.setSpacing(19);
        content.setMaxWidth(410);

        VBox panel = new VBox(content);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(64, 52, 64, 72));
        panel.setPrefWidth(520);
        panel.getStyleClass().add("auth-brand-panel");
        return panel;
    }

    private StackPane createAuthPanel() {
        loginTab.setMnemonicParsing(false);
        registerTab.setMnemonicParsing(false);
        loginTab.setOnAction(event -> switchMode(Mode.LOGIN));
        registerTab.setOnAction(event -> switchMode(Mode.REGISTER));
        loginTab.getStyleClass().add("auth-tab");
        registerTab.getStyleClass().add("auth-tab");

        HBox tabs = new HBox(loginTab, registerTab);
        tabs.setSpacing(3);
        tabs.getStyleClass().add("auth-tabs");

        formHost.setSpacing(18);
        formHost.getStyleClass().add("auth-form-host");

        status.setWrapText(true);
        status.setManaged(false);
        status.setVisible(false);
        status.getStyleClass().add("auth-status");

        progress.setPrefSize(16, 16);
        progress.setMinSize(16, 16);
        progress.setMaxSize(16, 16);
        progress.setVisible(false);
        progress.setManaged(false);

        HBox statusRow = new HBox(status, progress);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setSpacing(9);

        VBox card = new VBox(tabs, formHost, statusRow);
        card.setSpacing(25);
        card.setMaxWidth(450);
        card.setPadding(new Insets(31, 34, 34, 34));
        card.getStyleClass().add("auth-card");

        StackPane area = new StackPane(card);
        area.setPadding(new Insets(42, 74, 42, 74));
        area.getStyleClass().add("auth-form-area");
        return area;
    }

    private void switchMode(Mode nextMode) {
        if (busy) {
            return;
        }
        mode = nextMode;
        clearStatus();
        formHost.getChildren().setAll(nextMode == Mode.LOGIN ? createLoginForm() : createRegisterForm());
        loginTab.getStyleClass().remove("auth-tab-active");
        registerTab.getStyleClass().remove("auth-tab-active");
        (nextMode == Mode.LOGIN ? loginTab : registerTab).getStyleClass().add("auth-tab-active");
    }

    private VBox createLoginForm() {
        Label title = new Label("Welcome back");
        title.getStyleClass().add("auth-form-title");
        Label subtitle = new Label("Log in to continue exploring MyShop.");
        subtitle.getStyleClass().add("auth-form-subtitle");

        TextField email = new TextField();
        email.setPromptText("you@example.com");
        email.setAccessibleText("Email address");
        Label emailError = errorLabel();

        PasswordField password = new PasswordField();
        password.setPromptText("Your password");
        password.setAccessibleText("Password");
        Label passwordError = errorLabel();

        Button submit = new Button("Log in");
        submit.setMnemonicParsing(false);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.getStyleClass().add("primary-button");
        Runnable submitAction = () -> submitLogin(email, emailError, password, passwordError, submit);
        submit.setOnAction(event -> submitAction.run());
        password.setOnAction(event -> submitAction.run());

        Label switchText = new Label("New to MyShop?");
        switchText.getStyleClass().add("auth-switch-text");
        Button createAccount = new Button("Create an account");
        createAccount.setMnemonicParsing(false);
        createAccount.getStyleClass().add("auth-link-button");
        createAccount.setOnAction(event -> switchMode(Mode.REGISTER));
        HBox switchRow = new HBox(switchText, createAccount);
        switchRow.setAlignment(Pos.CENTER_LEFT);
        switchRow.setSpacing(6);

        VBox form = new VBox(
                title,
                subtitle,
                fieldBlock("Email", email, emailError),
                fieldBlock("Password", password, passwordError),
                submit,
                switchRow
        );
        form.setSpacing(13);
        form.getStyleClass().add("auth-form");
        return form;
    }

    private VBox createRegisterForm() {
        Label title = new Label("Create your account");
        title.getStyleClass().add("auth-form-title");
        Label subtitle = new Label("A few details are all you need to get started.");
        subtitle.getStyleClass().add("auth-form-subtitle");

        TextField name = new TextField();
        name.setPromptText("Your name");
        name.setAccessibleText("Name");
        Label nameError = errorLabel();

        TextField email = new TextField();
        email.setPromptText("you@example.com");
        email.setAccessibleText("Email address");
        Label emailError = errorLabel();

        PasswordField password = new PasswordField();
        password.setPromptText("At least 8 characters");
        password.setAccessibleText("Password");
        Label passwordError = errorLabel();

        PasswordField confirm = new PasswordField();
        confirm.setPromptText("Repeat your password");
        confirm.setAccessibleText("Confirm password");
        Label confirmError = errorLabel();

        Button submit = new Button("Create account");
        submit.setMnemonicParsing(false);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.getStyleClass().add("primary-button");
        Runnable submitAction = () -> submitRegistration(
                name, nameError, email, emailError, password, passwordError, confirm, confirmError, submit
        );
        submit.setOnAction(event -> submitAction.run());
        confirm.setOnAction(event -> submitAction.run());

        Label switchText = new Label("Already have an account?");
        switchText.getStyleClass().add("auth-switch-text");
        Button logIn = new Button("Log in");
        logIn.setMnemonicParsing(false);
        logIn.getStyleClass().add("auth-link-button");
        logIn.setOnAction(event -> switchMode(Mode.LOGIN));
        HBox switchRow = new HBox(switchText, logIn);
        switchRow.setAlignment(Pos.CENTER_LEFT);
        switchRow.setSpacing(6);

        VBox form = new VBox(
                title,
                subtitle,
                fieldBlock("Name", name, nameError),
                fieldBlock("Email", email, emailError),
                fieldBlock("Password", password, passwordError),
                fieldBlock("Confirm password", confirm, confirmError),
                submit,
                switchRow
        );
        form.setSpacing(11);
        form.getStyleClass().add("auth-form");
        return form;
    }

    private void submitLogin(
            TextField email,
            Label emailError,
            PasswordField password,
            Label passwordError,
            Button submit
    ) {
        clearStatus();
        clearError(email, emailError);
        clearError(password, passwordError);
        boolean valid = true;
        if (email.getText().isBlank()) {
            setError(email, emailError, "Enter your email address.");
            valid = false;
        } else if (!AuthService.isValidEmail(AuthService.normalizeEmail(email.getText()))) {
            setError(email, emailError, "Enter a valid email address.");
            valid = false;
        }
        if (password.getText().isEmpty()) {
            setError(password, passwordError, "Enter your password.");
            valid = false;
        }
        if (!valid) {
            return;
        }

        runAuthTask(
                submit,
                () -> controller.login(email.getText(), password.getText()),
                this::completeAuthentication
        );
    }

    private void submitRegistration(
            TextField name,
            Label nameError,
            TextField email,
            Label emailError,
            PasswordField password,
            Label passwordError,
            PasswordField confirm,
            Label confirmError,
            Button submit
    ) {
        clearStatus();
        clearError(name, nameError);
        clearError(email, emailError);
        clearError(password, passwordError);
        clearError(confirm, confirmError);
        boolean valid = true;
        if (name.getText().trim().isBlank()) {
            setError(name, nameError, "Enter your name.");
            valid = false;
        }
        if (email.getText().isBlank()) {
            setError(email, emailError, "Enter your email address.");
            valid = false;
        } else if (!AuthService.isValidEmail(AuthService.normalizeEmail(email.getText()))) {
            setError(email, emailError, "Enter a valid email address.");
            valid = false;
        }
        if (!AuthService.isValidPassword(password.getText())) {
            setError(password, passwordError, "Password must be at least 8 characters.");
            valid = false;
        }
        if (!password.getText().equals(confirm.getText())) {
            setError(confirm, confirmError, "Passwords do not match.");
            valid = false;
        }
        if (!valid) {
            return;
        }

        runAuthTask(
                submit,
                () -> controller.register(name.getText(), email.getText(), password.getText()),
                this::completeAuthentication
        );
    }

    private void runAuthTask(Button submit, AuthOperation operation, Consumer<User> success) {
        setBusy(true, submit);
        Task<User> task = new Task<>() {
            @Override
            protected User call() {
                return operation.execute();
            }
        };
        task.setOnSucceeded(event -> {
            setBusy(false, submit);
            success.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            setBusy(false, submit);
            Throwable exception = task.getException();
            if (exception instanceof AuthException authException) {
                showAuthError(authException);
            } else {
                showStatus("Something went wrong. Please try again.", true);
            }
        });
        Thread thread = new Thread(task, "myshop-auth-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void completeAuthentication(User user) {
        onAuthenticated.accept(user);
    }

    private void showAuthError(AuthException exception) {
        if (mode == Mode.LOGIN && exception.code() == AuthException.Code.INVALID_CREDENTIALS) {
            showStatus("Email or password is incorrect.", true);
            return;
        }
        showStatus(exception.getMessage(), true);
    }

    private VBox fieldBlock(String labelText, TextInputControl input, Label error) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        input.getStyleClass().add("auth-input");
        VBox block = new VBox(label, input, error);
        block.setSpacing(5);
        block.getStyleClass().add("field-block");
        return block;
    }

    private Label errorLabel() {
        Label error = new Label();
        error.setManaged(false);
        error.setVisible(false);
        error.getStyleClass().add("field-error");
        return error;
    }

    private void setError(TextInputControl field, Label error, String message) {
        field.getStyleClass().add("field-invalid");
        error.setText(message);
        error.setManaged(true);
        error.setVisible(true);
    }

    private void clearError(TextInputControl field, Label error) {
        field.getStyleClass().remove("field-invalid");
        error.setText("");
        error.setManaged(false);
        error.setVisible(false);
    }

    private void setBusy(boolean value, Button submit) {
        busy = value;
        submit.setDisable(value);
        loginTab.setDisable(value);
        registerTab.setDisable(value);
        submit.setText(value ? "Working…" : mode == Mode.LOGIN ? "Log in" : "Create account");
        progress.setManaged(value);
        progress.setVisible(value);
    }

    private void showStatus(String message, boolean error) {
        status.setText(message);
        status.getStyleClass().removeAll("auth-status", "auth-status-error");
        status.getStyleClass().add(error ? "auth-status-error" : "auth-status");
        status.setManaged(true);
        status.setVisible(true);
    }

    private void clearStatus() {
        status.setText("");
        status.setManaged(false);
        status.setVisible(false);
        status.getStyleClass().remove("auth-status-error");
        if (!status.getStyleClass().contains("auth-status")) {
            status.getStyleClass().add("auth-status");
        }
    }

    @FunctionalInterface
    private interface AuthOperation {
        User execute();
    }
}
