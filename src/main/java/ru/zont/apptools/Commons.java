package ru.zont.apptools;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class Commons {
    public static boolean isWindows() {
        return (System.getProperty("os.name").toLowerCase().contains("win"));
    }

    public static boolean isMac() {
        return (System.getProperty("os.name").toLowerCase().contains("mac"));
    }

    public static boolean isUnix() {
        final String os = System.getProperty("os.name");
        return (os.toLowerCase().contains("nix")
                || os.toLowerCase().contains("nux")
                || os.toLowerCase().contains("aix"));
    }

    public static void wrapErrors(WRunnable r) {
        wrapErrors(r, null);
    }

    public static void wrapErrors(WRunnable r, String err) {
        try {
            r.run();
        } catch (Throwable e) {
            reportError(err, e);
        }
    }

    public static void wrapErrorsAsync(WRunnable r) {
        wrapErrorsAsync(r, null);
    }

    public static void wrapErrorsAsync(WRunnable r, String err) {
        new Thread(() -> wrapErrors(r, err)).start();
    }

    public static Consumer<Throwable> onErrorWrapper() {
        return throwable -> reportError(null, throwable);
    }

    public static void reportError(String err, Throwable e) {
        Platform.runLater(() -> {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String st = sw.toString();
            System.err.println(st);


            new ErrorDialog(
                    err != null ? err : "Error",
                    st
            ).show();
        });
    }

    public static void fadeIn(Node node, int duration, EventHandler<ActionEvent> onFinish) {
        fade(node, 0f, 1f, duration, onFinish);
    }

    public static void fadeOut(Node node, int duration, EventHandler<ActionEvent> onFinish) {
        fade(node, 1f, 0f, duration, onFinish);
    }

    private static void fade(Node node, float start, float finish, int duration, EventHandler<ActionEvent> onFinish) {
        ObjectProperty<Float> value = new SimpleObjectProperty<>();
        KeyFrame[] keyValues = {
                new KeyFrame(Duration.ZERO, new KeyValue(value, start)),
                new KeyFrame(Duration.millis(duration), new KeyValue(value, finish))
        };
        Timeline timeline = new Timeline(keyValues);
        value.addListener((observable, oldValue, newValue) -> node.setStyle("-fx-opacity: " + newValue));
        if (onFinish != null) timeline.setOnFinished(onFinish);
        timeline.playFromStart();
    }

    public static void setValid(Labeled node, boolean valid) {
        String[] classes = new String[]{"text-valid", "text-invalid"};
        node.getStyleClass().removeAll(classes);

        String text, style;
        if (valid) {
            text = "Valid";
            style = classes[0];
        } else {
            text = "Invalid!";
            style = classes[1];
        }

        node.setText(text);
        node.getStyleClass().add(style);
    }

    public interface WRunnable {
        void run() throws Throwable;
    }

    /**
     * Attempts to calculate the size of a file or directory.
     *
     * <p>
     * Since the operation is non-atomic, the returned value may be inaccurate.
     * However, this method is quick and does its best.
     */
    public static long size(Path path) {

        final AtomicLong size = new AtomicLong(0);

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {

                    System.out.println("skipped: " + file + " (" + exc + ")");
                    // Skip folders that can't be traversed
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                    if (exc != null)
                        System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
                    // Ignore errors traversing a folder
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
        }

        return size.get();
    }

    public static Alert textAreaDialog(String title, String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        if (header != null)
            alert.setHeaderText(header);

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);

        alert.getDialogPane().setExpandableContent(expContent);

        return alert;
    }
}
