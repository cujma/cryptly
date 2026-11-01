package gui;

import cipher.SaveMode;
import operation.FileOperationHandler;
import operation.OperationCallbacks;
import operation.PathResolver;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public class MainView extends VBox {

    private final FileOperationHandler handler;
    private final PathSelectionView pathSelectionView = new PathSelectionView();
    private final PasswordView passwordView = new PasswordView();
    private final ProgressView progressView = new ProgressView();
    private final PathResolver pathResolver = new PathResolver();
    private final ToggleGroup operationGroup = new ToggleGroup();
    private final ToggleGroup saveGroup = new ToggleGroup();
    private final RadioButton encryptButton = new RadioButton("Encrypt");
    private final RadioButton decryptButton = new RadioButton("Decrypt");
    private final RadioButton replaceOriginal = new RadioButton("Replace original file");
    private final RadioButton saveNew = new RadioButton("Save as a new file");
    private final Button actionButton = new Button("Encrypt file");

    public MainView(FileOperationHandler handler) {
        this.handler = handler;
        getStyleClass().add("main-view");
        setAlignment(Pos.TOP_CENTER);
        setFillWidth(true);
        setSpacing(10);
        setPadding(new Insets(18, 20, 4, 20));

        configureForm();
        progressView.setBackRequested(this::showForm);
        getChildren().setAll(buildHeading(), operationSelector(), pathSelectionView,
                saveSection(), passwordView, actionButton, progressView);
    }

    private void configureForm() {
        encryptButton.setToggleGroup(operationGroup);
        decryptButton.setToggleGroup(operationGroup);
        encryptButton.setSelected(true);
        replaceOriginal.setToggleGroup(saveGroup);
        saveNew.setToggleGroup(saveGroup);
        saveNew.setSelected(true);

        actionButton.getStyleClass().add("primary-button");
        actionButton.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(actionButton, new Insets(8, 0, 2, 0));
        actionButton.setOnAction(event -> performOperation());

        operationGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            updateOperationMode();
            handleOperationChanged();
        });
        saveGroup.selectedToggleProperty()
                .addListener((observable, oldValue, newValue) -> updateSaveMode());
        updateOperationMode();
        updateSaveMode();
    }

    private VBox buildHeading() {
        Label title = new Label("Protect your data");
        title.getStyleClass().add("title");
        VBox heading = new VBox(title);
        heading.setAlignment(Pos.CENTER);
        return heading;
    }

    private HBox operationSelector() {
        HBox selector = new HBox(28, encryptButton, decryptButton);
        selector.getStyleClass().add("form-section");
        selector.setAlignment(Pos.CENTER);
        selector.setMaxWidth(Double.MAX_VALUE);
        return selector;
    }

    private VBox saveSection() {
        Label title = new Label("Save options");
        title.getStyleClass().add("section-title");
        VBox section = new VBox(8, title, new VBox(10, replaceOriginal, saveNew));
        section.getStyleClass().add("form-section");
        section.setMaxWidth(Double.MAX_VALUE);
        return section;
    }

    private boolean isEncryptSelected() {
        return operationGroup.getSelectedToggle() == encryptButton;
    }

    private boolean isSaveNewSelected() {
        return saveGroup.getSelectedToggle() == saveNew;
    }

    private void updateOperationMode() {
        boolean encrypt = isEncryptSelected();
        actionButton.setText(encrypt ? "Encrypt file" : "Decrypt file");
        passwordView.setConfirmationEnabled(encrypt);
        pathSelectionView.setFolderSelectionEnabled(encrypt);
    }

    private void updateSaveMode() {
        pathSelectionView.setOutputSelectionEnabled(isSaveNewSelected());
    }

    private void handleOperationChanged() {
        if (!isEncryptSelected() && !pathSelectionView.inputText().isBlank()) {
            try {
                if (Files.isDirectory(Path.of(pathSelectionView.inputText()))) {
                    pathSelectionView.clear();
                }
            } catch (RuntimeException exception) {
                pathSelectionView.clear();
            }
        }
    }

    private void performOperation() {
        boolean encrypt = isEncryptSelected();
        boolean separate = isSaveNewSelected();

        final Path input;
        try {
            input = pathResolver.resolveInput(pathSelectionView.inputText(), encrypt);
        } catch (IllegalArgumentException exception) {
            showValidationError(exception.getMessage());
            return;
        }

        String password = passwordView.password();
        if (password.isEmpty()) {
            showValidationError("Please enter a password.");
            return;
        }
        if (encrypt && !password.equals(passwordView.confirmation())) {
            showValidationError("Passwords do not match.");
            return;
        }

        final Path output;
        try {
            output = pathResolver.resolveOutput(
                    input, pathSelectionView.outputDirectoryText(), encrypt, separate
            );
        } catch (IllegalArgumentException exception) {
            showValidationError(exception.getMessage());
            return;
        }

        if (encrypt && !separate && !confirmReplacement(input)) {
            return;
        }

        char[] passwordChars = password.toCharArray();
        SaveMode saveMode = separate ? SaveMode.SEPARATE_FILE : SaveMode.REPLACE_ORIGINAL;
        showProgress(encrypt ? "Encrypting file..." : "Decrypting file...");

        try {
            OperationCallbacks callbacks = new OperationCallbacks(
                    progressView::appendMessage,
                    progressView::appendError,
                    progressView::updateProgress,
                    this::operationFinished
            );
            if (encrypt) {
                handler.encrypt(input, output, passwordChars, saveMode, callbacks);
            } else {
                handler.decrypt(input, output, passwordChars, saveMode, callbacks);
            }
        } catch (RuntimeException exception) {
            progressView.appendError("Could not start operation: " + safeMessage(exception));
            operationFinished();
        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    private void showProgress(String title) {
        progressView.start(title);
        setFormVisible(false);
        progressView.setVisible(true);
        progressView.setManaged(true);
    }

    private void operationFinished() {
        progressView.finish();
        pathSelectionView.clear();
    }

    private void setFormVisible(boolean visible) {
        getChildren().stream()
                .filter(child -> child != progressView)
                .forEach(child -> {
                    child.setVisible(visible);
                    child.setManaged(visible);
                });
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("");
        alert.setHeaderText("Cannot start operation");
        alert.setContentText(message);
        alert.initOwner(getStage());
        alert.getDialogPane().getStylesheets()
                .add(getClass().getResource("/style.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("error-alert");
        alert.showAndWait();
    }

    private boolean confirmReplacement(Path input) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("");
        alert.setHeaderText("Original data will be replaced");
        alert.setContentText(
                "After successful encryption, the original "
                        + (Files.isDirectory(input) ? "folder and its contents" : "file")
                        + " will be permanently deleted. Continue?"
        );
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        alert.initOwner(getStage());
        alert.getDialogPane().getStylesheets()
                .add(getClass().getResource("/style.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("warning-alert");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showForm() {
        progressView.setVisible(false);
        progressView.setManaged(false);
        setFormVisible(true);
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName()
                : message;
    }

    private Stage getStage() {
        return (Stage) getScene().getWindow();
    }
}
