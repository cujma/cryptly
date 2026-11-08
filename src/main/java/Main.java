
import gui.MainView;
import gui.WindowFrame;
import operation.FileOperationController;
import operation.FileOperationService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {
    private final FileOperationController operationController =
            new FileOperationController(new FileOperationService());

    @Override
    public void start(Stage stage) {
        MainView view = new MainView(operationController);

        Scene scene = new Scene(new WindowFrame(stage, view), 440, 690);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.getIcons().add(
                new Image(getClass().getResourceAsStream("/icons/icon.png"))
        );
        stage.setTitle("Cryptly");
        stage.setMinWidth(420);
        stage.setMinHeight(690);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            if (view.isOperationRunning()) {
                event.consume();
            }
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
