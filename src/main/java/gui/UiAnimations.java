package gui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.Interpolator;
import javafx.scene.Node;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.util.Duration;

import java.util.List;

final class UiAnimations {
    private static final Duration TRANSITION_DURATION = Duration.millis(220);
    private static final double SLIDE_DISTANCE = 8;

    private UiAnimations() {
    }

    static void enter(Node node) {
        if (isReducedMotion()) {
            reset(node);
            return;
        }

        node.setOpacity(0);
        node.setTranslateY(SLIDE_DISTANCE);
        new ParallelTransition(fade(node, 1), translate(node, 0)).play();
    }

    static void installButtonFeedback(Node button) {
        button.setOnMousePressed(event -> scale(button, 0.97).play());
        button.setOnMouseReleased(event -> scale(button, 1).play());
        button.setOnMouseExited(event -> scale(button, 1).play());
    }

    static void swap(Node current, Node next) {
        if (isReducedMotion()) {
            current.setVisible(false);
            current.setManaged(false);
            next.setVisible(true);
            next.setManaged(true);
            return;
        }

        FadeTransition hide = fade(current, 0);
        hide.setOnFinished(event -> {
            current.setVisible(false);
            current.setManaged(false);
            next.setVisible(true);
            next.setManaged(true);
            enter(next);
        });
        hide.play();
    }

    static void minimize(Node frame, javafx.stage.Stage stage) {
        if (isReducedMotion()) {
            stage.setIconified(true);
            return;
        }

        Rectangle2D screen = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()).stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
        double targetX = screen.getMinX() + screen.getWidth() / 2 - stage.getX() - stage.getWidth() / 2;
        double targetY = screen.getMaxY() - stage.getY() - stage.getHeight() / 2;

        ScaleTransition shrink = scale(frame, 0.08);
        TranslateTransition move = new TranslateTransition(Duration.millis(380), frame);
        move.setToX(targetX);
        move.setToY(targetY);
        move.setInterpolator(Interpolator.EASE_IN);
        shrink.setInterpolator(Interpolator.EASE_IN);
        ParallelTransition transition = new ParallelTransition(shrink, move, fade(frame, 0.35));

        transition.setOnFinished(event -> {
            stage.setIconified(true);
            frame.setOpacity(1);
            frame.setScaleX(1);
            frame.setScaleY(1);
            frame.setTranslateX(0);
            frame.setTranslateY(0);
        });
        transition.play();
    }

    static void close(Node frame, javafx.stage.Stage stage) {
        if (isReducedMotion()) {
            stage.close();
            return;
        }

        ParallelTransition transition = new ParallelTransition(fade(frame, 0), scale(frame, 0.97));

        transition.setOnFinished(event -> stage.close());
        transition.play();
    }

    static void enterAll(List<? extends Node> nodes) {
        nodes.forEach(UiAnimations::enter);
    }

    static void exitAll(List<? extends Node> nodes) {
        if (isReducedMotion()) {
            nodes.forEach(node -> {
                reset(node);
                node.setVisible(false);
                node.setManaged(false);
            });
            return;
        }

        ParallelTransition transition = new ParallelTransition();
        nodes.forEach(node -> {
            node.setMouseTransparent(true);
            transition.getChildren().addAll(fade(node, 0), translate(node, -SLIDE_DISTANCE));
        });

        transition.setOnFinished(event -> {
            nodes.forEach(node -> {
                node.setVisible(false);
                node.setManaged(false);
                node.setMouseTransparent(false);
                reset(node);
            });
        });
        transition.play();
    }

    private static FadeTransition fade(Node node, double toValue) {
        FadeTransition transition = new FadeTransition(TRANSITION_DURATION, node);
        transition.setToValue(toValue);
        return transition;
    }

    private static TranslateTransition translate(Node node, double toValue) {
        TranslateTransition transition = new TranslateTransition(TRANSITION_DURATION, node);
        transition.setToY(toValue);
        return transition;
    }

    private static ScaleTransition scale(Node node, double toValue) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(110), node);
        transition.setToX(toValue);
        transition.setToY(toValue);
        return transition;
    }

    private static void reset(Node node) {
        node.setOpacity(1);
        node.setTranslateY(0);
    }

    private static boolean isReducedMotion() {
        return Boolean.getBoolean("cryptly.reduceMotion");
    }
}
