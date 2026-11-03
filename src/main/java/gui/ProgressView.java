package gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ProgressView extends VBox {

    private final Label operationTitle = new Label("Processing file...");
    private final Label status = new Label("Preparing operation...");
    private final ProgressBar progressBar = new ProgressBar();
    private final TextArea messagesArea = new TextArea();
    private final Button backButton = new Button("Back to file selection");
    private Runnable backRequested = () -> {
    };

    public ProgressView() {
        getStyleClass().add("progress-view");
        setSpacing(10);
        setAlignment(Pos.TOP_CENTER);
        setFillWidth(true);
        setVisible(false);
        setManaged(false);

        operationTitle.getStyleClass().add("title");
        status.getStyleClass().add("status-label");
        status.setWrapText(true);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(14);

        VBox progressSection = new VBox(8, progressBar, status);
        progressSection.getStyleClass().addAll("form-section", "progress-section");
        progressSection.setMaxWidth(Double.MAX_VALUE);

        messagesArea.setEditable(false);
        messagesArea.setWrapText(true);
        messagesArea.setPromptText("Errors and messages will appear here...");
        messagesArea.setPrefRowCount(10);
        messagesArea.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(messagesArea, Priority.ALWAYS);

        Label messagesTitle = new Label("Errors and messages");
        messagesTitle.getStyleClass().add("section-title");
        VBox messagesSection = new VBox(8, messagesTitle, messagesArea);
        messagesSection.getStyleClass().addAll("form-section", "progress-messages");
        messagesSection.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(messagesSection, Priority.ALWAYS);
        VBox.setMargin(messagesSection, new Insets(0, 0, 0, 0));

        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.getStyleClass().add("menu-button");
        backButton.setDisable(true);
        backButton.setOnAction(event -> backRequested.run());
        VBox.setMargin(backButton, new Insets(8, 0, 16, 0));

        getChildren().setAll(
                heading(), progressSection, messagesSection, backButton
        );
    }

    public void setBackRequested(Runnable backRequested) {
        this.backRequested = backRequested;
    }

    public void start(String title) {
        operationTitle.setText(title);
        status.setText("Please wait...");
        messagesArea.clear();
        messagesArea.appendText("[INFO] Operation started.\n");
        progressBar.setProgress(0);
        backButton.setDisable(true);
    }

    public void updateProgress(double progress) {
        if (Double.isFinite(progress)) {
            progressBar.setProgress(Math.max(0, Math.min(1, progress)));
            status.setText(String.format("Progress: %.0f%%", progress * 100));
        }
    }

    public void appendMessage(String message) {
        status.setText(message);
        messagesArea.appendText("[INFO] " + message + "\n");
    }

    public void appendError(String message) {
        messagesArea.appendText("[ERROR] " + message + "\n");
        status.setText("Operation failed.");
    }

    public void finish() {
        progressBar.setProgress(1.0);
        backButton.setDisable(false);
    }

    private VBox heading() {
        VBox heading = new VBox(operationTitle);
        heading.setAlignment(Pos.CENTER);
        return heading;
    }
}
