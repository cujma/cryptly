package gui;

import operation.FileOperationController;
import operation.OperationCallbacks;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class MainView extends VBox {
    private final FileOperationController operationController;
    private final ErrorMessageProvider errorMessageProvider = new ErrorMessageProvider();
    private boolean operationRunning;
    private final PathSelectionView pathSelectionView = new PathSelectionView();
    private final PasswordView passwordView = new PasswordView();
    private final ProgressView progressView = new ProgressView();
    private final ToggleGroup operationGroup = new ToggleGroup();
    private final ToggleGroup saveGroup = new ToggleGroup();
    private final RadioButton encryptButton = new RadioButton("Encrypt");
    private final RadioButton decryptButton = new RadioButton("Decrypt");
    private final Label operationTitle = new Label();
    private final RadioButton replaceOriginal = new RadioButton("Replace original file");
    private final RadioButton saveNew = new RadioButton("Save as a new file");
    private final Button actionButton = new Button("Encrypt");
    private List<Node> formNodes;

    public MainView(FileOperationController operationController) {
        this.operationController = operationController;
        getStyleClass().add("main-view");
        setAlignment(Pos.TOP_CENTER);
        setFillWidth(true);
        setSpacing(10);
        setPadding(new Insets(18, 20, 4, 20));

        configureForm();
        VBox.setVgrow(progressView, Priority.ALWAYS);
        progressView.setBackRequested(this::showForm);
        getChildren().setAll(buildHeading(), operationSelector(), pathSelectionView, saveSection(), passwordView, actionButton, progressView);
        formNodes = getChildren().stream()
                .filter(child -> child != progressView)
                .toList();
        Platform.runLater(() -> UiAnimations.enterAll(formNodes));
    }

    private void configureForm() {
        encryptButton.setToggleGroup(operationGroup);
        decryptButton.setToggleGroup(operationGroup);
        encryptButton.setSelected(true);
        replaceOriginal.setToggleGroup(saveGroup);
        saveNew.setToggleGroup(saveGroup);
        saveNew.setSelected(true);

        actionButton.getStyleClass().add("primary-button");
        UiAnimations.installButtonFeedback(actionButton);
        actionButton.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(actionButton, new Insets(8, 0, 2, 0));
        actionButton.setOnAction(event -> performOperation());

        operationGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {updateOperationMode();handleOperationChanged();});
        saveGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> updateSaveMode());
        updateOperationMode();
        updateSaveMode();
    }

    private VBox buildHeading() {
        operationTitle.getStyleClass().add("title");
        VBox heading = new VBox(operationTitle);
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
        operationTitle.setText(encrypt ? "Encrypt your data" : "Decrypt your data");
        UiAnimations.enter(operationTitle);
        actionButton.setText(encrypt ? "Encrypt" : "Decrypt");
        passwordView.setConfirmationEnabled(encrypt);
        pathSelectionView.setFolderSelectionEnabled(encrypt);
        saveNew.setDisable(!encrypt);
        if (!encrypt) {
            replaceOriginal.setSelected(true);
        }
    }

    private void updateSaveMode() {
        pathSelectionView.setOutputSelectionEnabled(isSaveNewSelected());
    }

    private void handleOperationChanged() {
        if (!isEncryptSelected() && !pathSelectionView.inputText().isBlank()) {
            if (operationController.isDirectory(pathSelectionView.inputText())) {
                pathSelectionView.clear();
            }
        }
    }

    private void performOperation() {
        boolean encrypt = isEncryptSelected();
        boolean separate = isSaveNewSelected();

        char[] password = passwordView.password();
        char[] confirmation = passwordView.confirmation();

        try {
            OperationCallbacks callbacks = new OperationCallbacks(
                    progressView::appendMessage,
                    message -> progressView.appendError(errorMessageProvider.messageFor(message)),
                    progressView::updateProgress,
                    this::operationFinished
            );
            FileOperationController.PreparedOperation operation = operationController.prepare(
                    pathSelectionView.inputText(),
                    pathSelectionView.outputDirectoryText(),
                    encrypt,
                    separate,
                    password,
                    confirmation,
                    this::confirmReplacement
            );
            if (operation == null) {
                return;
            }
            showProgress(encrypt ? "Encrypting file..." : "Decrypting file...");
            operationController.start(operation, password, callbacks);
        } catch (Exception exception) {
            if (exception instanceof operation.OperationException
                    || exception instanceof IllegalArgumentException) {
                showValidationError(errorMessageProvider.messageFor(exception));
                return;
            }
            progressView.appendError(errorMessageProvider.messageFor(exception));
            operationFinished();
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }

    private void showProgress(String title) {
        operationRunning = true;
        progressView.start(title);
        progressView.setVisible(true);
        progressView.setManaged(true);
        UiAnimations.enter(progressView);
        UiAnimations.exitAll(formNodes);
    }

    private void operationFinished() {
        operationRunning = false;
        progressView.finish();
        pathSelectionView.clear();
        passwordView.clear();
    }

    private void setFormVisible(boolean visible) {
        formNodes.forEach(child -> {
                    child.setVisible(visible);
                    child.setManaged(visible);
                });
    }

    private void showValidationError(String message) {
        Stage dialog = createDialog("Cannot start operation", message);
        Button close = dialogButton("OK", dialog);
        HBox buttons = new HBox(close);
        buttons.getStyleClass().add("dialog-buttons");
        buttons.setAlignment(Pos.CENTER_RIGHT);
        dialogContent(dialog).getChildren().add(buttons);
        dialog.showAndWait();
    }

    private boolean confirmReplacement(Path input) {
        Stage dialog = createDialog("Original data will be replaced", "After successful encryption, the original " + (Files.isDirectory(input) ? "folder and its contents" : "file") + " will be permanently deleted.");
        HBox buttons = new HBox(8);
        buttons.getStyleClass().add("dialog-buttons");
        Button cancel = dialogButton("Cancel", dialog);
        Button confirm = dialogButton("Continue", dialog);
        final boolean[] accepted = {false};
        confirm.setOnAction(event -> {
            accepted[0] = true;
            dialog.close();
        });
        buttons.getChildren().addAll(cancel, confirm);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        dialogContent(dialog).getChildren().add(buttons);
        dialog.showAndWait();

        return accepted[0];
    }

    private Stage createDialog(String title, String message) {
        Label heading = new Label(title);
        heading.getStyleClass().add("dialog-heading");
        heading.setAlignment(Pos.CENTER_LEFT);
        heading.setMaxWidth(Double.MAX_VALUE);
        Label content = new Label(message);
        content.getStyleClass().add("dialog-content");
        content.setWrapText(true);
        content.setMaxWidth(Double.MAX_VALUE);
        Separator separator = new Separator();
        separator.getStyleClass().add("dialog-separator");
        VBox panel = new VBox(12, heading, content);
        panel.getChildren().add(1, separator);
        VBox.setMargin(separator, new Insets(-10, 0, -5, 0));
        panel.getStyleClass().add("dialog-panel");
        panel.setPrefWidth(340);

        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initOwner(getStage());
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        WindowFrame frame = new WindowFrame(dialog, "Cryptly", panel, false);
        frame.getStyleClass().add("dialog-window-frame");
        frame.setPrefWidth(370);
        Scene scene = new Scene(frame);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.sizeToScene();
        dialog.setOnShown(event -> UiAnimations.enter(frame));
        return dialog;
    }

    private Button dialogButton(String text, Stage dialog) {
        Button button = new Button(text);
        button.getStyleClass().add("dialog-button");
        UiAnimations.installButtonFeedback(button);
        button.setOnAction(event -> dialog.close());

        return button;
    }

    private VBox dialogContent(Stage dialog) {
        return (VBox) ((WindowFrame) dialog.getScene().getRoot()).getCenter();
    }

    private void showForm() {
        progressView.setVisible(false);
        progressView.setManaged(false);
        setFormVisible(true);
        UiAnimations.enterAll(formNodes);
    }

    private Stage getStage() {
        return (Stage) getScene().getWindow();
    }

    public boolean isOperationRunning() {
        return operationRunning;
    }
}
