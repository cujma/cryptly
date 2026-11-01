package gui;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Arrays;

public class PasswordView extends VBox {

    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmationField = new PasswordField();
    private final TextField visiblePasswordField = new TextField();
    private final TextField visibleConfirmationField = new TextField();
    private final CheckBox showPassword = new CheckBox("Show password");

    public PasswordView() {
        setSpacing(8);
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("form-section");

        passwordField.setPromptText("Enter password");
        confirmationField.setPromptText("Confirm password");
        visiblePasswordField.setPromptText("Enter password");
        visibleConfirmationField.setPromptText("Confirm password");

        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
        visibleConfirmationField.setVisible(false);
        visibleConfirmationField.setManaged(false);

        passwordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());
        confirmationField.textProperty().bindBidirectional(visibleConfirmationField.textProperty());
        showPassword.selectedProperty().addListener((observable, oldValue, show) -> setPasswordVisible(show));

        Label title = new Label("Password");
        title.getStyleClass().add("section-title");
        getChildren().addAll(
                title,
                new VBox(9, new StackPane(passwordField, visiblePasswordField),
                        new StackPane(confirmationField, visibleConfirmationField)),
                showPassword
        );
    }

    public char[] password() {
        return visiblePasswordField.isVisible()
                ? visiblePasswordField.getText().toCharArray()
                : passwordField.getText().toCharArray();
    }

    public char[] confirmation() {
        return visibleConfirmationField.isVisible()
                ? visibleConfirmationField.getText().toCharArray()
                : confirmationField.getText().toCharArray();
    }

    public void clear() {
        passwordField.clear();
        confirmationField.clear();
        visiblePasswordField.clear();
        visibleConfirmationField.clear();
    }

    public void setConfirmationEnabled(boolean enabled) {
        confirmationField.setDisable(!enabled);
        visibleConfirmationField.setDisable(!enabled);
    }

    private void setPasswordVisible(boolean visible) {
        passwordField.setVisible(!visible);
        passwordField.setManaged(!visible);
        visiblePasswordField.setVisible(visible);
        visiblePasswordField.setManaged(visible);
        confirmationField.setVisible(!visible);
        confirmationField.setManaged(!visible);
        visibleConfirmationField.setVisible(visible);
        visibleConfirmationField.setManaged(visible);
    }
}
