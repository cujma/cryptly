package gui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.nio.file.Path;

public class PathSelectionView extends VBox {

    private final TextField inputField = new TextField();
    private final TextField outputDirectoryField = new TextField();
    private final Button folderButton = new Button("Folder");
    private final Button outputDirectoryButton = new Button("Browse");

    public PathSelectionView() {
        setSpacing(8);
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("form-section");

        inputField.setEditable(false);
        inputField.setPromptText("Choose a file...");
        outputDirectoryField.setEditable(false);
        outputDirectoryField.setPromptText("Output directory...");

        Button fileButton = new Button("File");
        fileButton.getStyleClass().add("menu-button");
        folderButton.getStyleClass().add("menu-button");
        outputDirectoryButton.getStyleClass().add("menu-button");
        fileButton.setOnAction(event -> chooseInputFile());
        folderButton.setOnAction(event -> chooseInputDirectory());
        outputDirectoryButton.setOnAction(event -> chooseOutputDirectory());

        HBox inputRow = new HBox(8, inputField, fileButton, folderButton);
        inputRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        HBox outputRow = new HBox(8, outputDirectoryField, outputDirectoryButton);
        outputRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(outputDirectoryField, Priority.ALWAYS);

        Label title = new Label("File locations");
        title.getStyleClass().add("section-title");
        getChildren().addAll(
                title,
                fieldGroup("Input file", inputRow),
                fieldGroup("Output directory", outputRow)
        );
    }

    public String inputText() {
        return inputField.getText();
    }

    public String outputDirectoryText() {
        return outputDirectoryField.getText();
    }

    public void clear() {
        inputField.clear();
        outputDirectoryField.clear();
    }

    public void setFolderSelectionEnabled(boolean enabled) {
        folderButton.setDisable(!enabled);
    }

    public void setOutputSelectionEnabled(boolean enabled) {
        outputDirectoryField.setDisable(!enabled);
        outputDirectoryButton.setDisable(!enabled);
    }

    private VBox fieldGroup(String title, HBox content) {
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        return new VBox(5, label, content);
    }

    private void chooseInputFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select input file");
        File selected = chooser.showOpenDialog(getStage());
        if (selected == null) {
            return;
        }

        Path input = selected.toPath().toAbsolutePath().normalize();
        inputField.setText(input.toString());
        setOutputToParent(input);
    }

    private void chooseInputDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select input directory");
        File selected = chooser.showDialog(getStage());
        if (selected == null) {
            return;
        }

        Path input = selected.toPath().toAbsolutePath().normalize();
        inputField.setText(input.toString());
        setOutputToParent(input);
    }

    private void setOutputToParent(Path input) {
        outputDirectoryField.setText(
                input.getParent() == null ? input.toString() : input.getParent().toString()
        );
    }

    private void chooseOutputDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select output directory");

        String current = outputDirectoryField.getText();
        if (!current.isBlank()) {
            File directory = new File(current);
            if (directory.isDirectory()) {
                chooser.setInitialDirectory(directory);
            }
        }

        File selected = chooser.showDialog(getStage());
        if (selected != null) {
            outputDirectoryField.setText(selected.getAbsolutePath());
        }
    }

    private Stage getStage() {
        return (Stage) getScene().getWindow();
    }
}
