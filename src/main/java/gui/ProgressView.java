package gui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ProgressView extends VBox {

    private final Label progressTitle = new Label("Processing file...");
    private final Label status = new Label("Preparing operation...");
    private final ProgressBar progressBar = new ProgressBar();
    private final TextArea messagesArea = new TextArea();
    private final Button backButton = new Button("Back to file selection");
    private Runnable backRequested = () -> {
    };

    public ProgressView() {
        setSpacing(16);
        setAlignment(Pos.TOP_CENTER);
        setFillWidth(true);
        setVisible(false);
        setManaged(false);

        progressTitle.getStyleClass().add("section-title");
        status.getStyleClass().add("status-label");
        status.setWrapText(true);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(14);

        messagesArea.setEditable(false);
        messagesArea.setWrapText(true);
        messagesArea.setPromptText("Errors and messages will appear here...");
        messagesArea.setPrefRowCount(10);
        messagesArea.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(messagesArea, Priority.ALWAYS);

        Label messagesTitle = new Label("Errors and messages");
        messagesTitle.getStyleClass().add("section-title");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setDisable(true);
        backButton.setOnAction(event -> backRequested.run());

        getChildren().setAll(
                heading(), progressTitle, progressBar, status,
                messagesTitle, messagesArea, backButton
        );
    }

    public void setBackRequested(Runnable backRequested) {
        this.backRequested = backRequested;
    }

    public void start(String title) {
        progressTitle.setText(title);
        status.setText("Please wait...");
        messagesArea.clear();
        messagesArea.appendText("Operation started.\n");
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
        Label title = new Label("Protect your data");
        title.getStyleClass().add("title");
        Label subtitle = new Label("Your operation is in progress");
        subtitle.getStyleClass().add("subtitle");
        VBox heading = new VBox(6, title, subtitle);
        heading.setAlignment(Pos.CENTER);
        return heading;
    }
}
