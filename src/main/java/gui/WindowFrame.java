package gui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;

public class WindowFrame extends BorderPane {
    private double dragOffsetX;
    private double dragOffsetY;

    public WindowFrame(Stage stage, MainView content) {
        this(stage, "Cryptly", content, true);
    }

    public WindowFrame(Stage stage, String titleText, Pane content) {
        this(stage, titleText, content, true);
    }

    public WindowFrame(Stage stage, String titleText, Pane content, boolean showControls) {
        getStyleClass().add("window-frame");
        Rectangle clip = new Rectangle();
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
        setTop(createTitleBar(stage, titleText, showControls));
        setCenter(content);
    }

    private BorderPane createTitleBar(Stage stage, String titleText, boolean showControls) {
        Label title = new Label(titleText);
        title.getStyleClass().add("window-title");

        HBox controls = new HBox();
        controls.getStyleClass().add("window-controls");
        if (showControls) {
        SVGPath minimizeSymbol = new SVGPath();
        minimizeSymbol.setContent("M 3 6 L 13 6");
        minimizeSymbol.setScaleX(0.66);
        minimizeSymbol.setScaleY(0.66);
        minimizeSymbol.setStrokeLineCap(StrokeLineCap.ROUND);
        minimizeSymbol.setStrokeLineJoin(StrokeLineJoin.ROUND);
        minimizeSymbol.getStyleClass().addAll("window-symbol", "window-minimize-symbol");
        Button minimize = new Button();
        minimize.setGraphic(minimizeSymbol);
        minimize.getStyleClass().addAll("window-button", "window-minimize-button");
        minimize.setOnAction(event -> stage.setIconified(true));

        SVGPath closeSymbol = new SVGPath();
        closeSymbol.setContent("M 3 3 L 13 13 M 13 3 L 3 13");
        closeSymbol.setScaleX(0.66);
        closeSymbol.setScaleY(0.66);
        closeSymbol.setStrokeLineCap(StrokeLineCap.ROUND);
        closeSymbol.setStrokeLineJoin(StrokeLineJoin.ROUND);
        closeSymbol.getStyleClass().addAll("window-symbol", "window-close-symbol");
        Button close = new Button();
        close.setGraphic(closeSymbol);
        close.getStyleClass().addAll("window-button", "window-close-button");
        close.setOnAction(event -> stage.close());
            controls.getChildren().addAll(minimize, close);
        }

        BorderPane titleBar = new BorderPane();
        titleBar.getStyleClass().add("title-bar");
        titleBar.setLeft(title);
        if (showControls) {
            titleBar.setRight(controls);
        }
        BorderPane.setAlignment(title, Pos.CENTER_LEFT);
        controls.setAlignment(Pos.CENTER_RIGHT);

        titleBar.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!isControl(event.getPickResult().getIntersectedNode(), controls)) {
                dragOffsetX = event.getScreenX() - stage.getX();
                dragOffsetY = event.getScreenY() - stage.getY();
            }
        });
        titleBar.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!isControl(event.getPickResult().getIntersectedNode(), controls)) {
                stage.setX(event.getScreenX() - dragOffsetX);
                stage.setY(event.getScreenY() - dragOffsetY);
            }
        });

        return titleBar;
    }

    private boolean isControl(Node node, HBox controls) {
        while (node != null) {
            if (node == controls) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }
}
