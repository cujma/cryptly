package gui;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class PasswordView extends VBox {
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmationField = new PasswordField();
    private final ProgressBar strengthBar = new ProgressBar(0);
    private final Label strengthLabel = new Label("Password strength");

    public PasswordView() {
        setSpacing(8);
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("form-section");

        passwordField.setPromptText("Enter password");
        confirmationField.setPromptText("Confirm password");
        strengthBar.setMaxWidth(Double.MAX_VALUE);
        strengthBar.getStyleClass().addAll("password-strength-bar", "empty");
        strengthLabel.getStyleClass().add("password-strength-label");

        passwordField.textProperty().addListener((observable, oldValue, newValue) -> updateStrength(newValue));

        Label title = new Label("Password");
        title.getStyleClass().add("section-title");
        VBox fields = new VBox(9,
                passwordField,
                confirmationField,
                strengthBar,
                strengthLabel
        );
        getChildren().addAll(title, fields);
    }

    public char[] password() {
        return passwordField.getText().toCharArray();
    }

    public char[] confirmation() {
        return confirmationField.getText().toCharArray();
    }

    public void clear() {
        passwordField.clear();
        confirmationField.clear();
        updateStrength("");
    }

    public void setConfirmationEnabled(boolean enabled) {
        confirmationField.setDisable(!enabled);
    }

    private void updateStrength(String password) {
        Strength strength = Strength.forPassword(password);
        strengthBar.setProgress(strength.progress);
        strengthLabel.setText(strength.label);
        strengthBar.getStyleClass().removeIf(styleClass -> styleClass.equals("empty")
                || styleClass.equals("very-weak")
                || styleClass.equals("weak")
                || styleClass.equals("medium")
                || styleClass.equals("strong")
                || styleClass.equals("very-strong"));
        strengthLabel.getStyleClass().removeIf(styleClass -> styleClass.equals("empty")
                || styleClass.equals("very-weak")
                || styleClass.equals("weak")
                || styleClass.equals("medium")
                || styleClass.equals("strong")
                || styleClass.equals("very-strong"));
        strengthBar.getStyleClass().add(strength.styleClass);
        strengthLabel.getStyleClass().add(strength.styleClass);
    }

    private enum Strength {
        EMPTY(0, "Password strength", "empty"),
        VERY_WEAK(0.2, "Very weak", "very-weak"),
        WEAK(0.4, "Weak", "weak"),
        MEDIUM(0.6, "Medium", "medium"),
        STRONG(0.8, "Strong", "strong"),
        VERY_STRONG(1.0, "Very strong", "very-strong");

        private final double progress;
        private final String label;
        private final String styleClass;

        Strength(double progress, String label, String styleClass) {
            this.progress = progress;
            this.label = label;
            this.styleClass = styleClass;
        }

        private static Strength forPassword(String password) {
            if (password == null || password.isEmpty()) {
                return EMPTY;
            }

            int score = 0;
            if (password.length() >= 8) {
                score++;
            }
            if (password.length() >= 12) {
                score++;
            }
            if (password.chars().anyMatch(Character::isLowerCase)) {
                score++;
            }
            if (password.chars().anyMatch(Character::isUpperCase)) {
                score++;
            }
            if (password.chars().anyMatch(Character::isDigit)) {
                score++;
            }
            if (password.chars().anyMatch(character -> !Character.isLetterOrDigit(character))) {
                score++;
            }

            if (score <= 1) {
                return VERY_WEAK;
            }
            if (score == 2) {
                return WEAK;
            }
            if (score == 3) {
                return MEDIUM;
            }
            if (score == 4) {
                return STRONG;
            }
            return VERY_STRONG;
        }
    }
}
