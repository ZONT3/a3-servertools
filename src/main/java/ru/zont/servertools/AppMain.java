package ru.zont.servertools;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.zont.modsextractor.ui.MEStage;
import ru.zont.musicpacker.ui.MPStage;

public class AppMain extends Application {

    private MainController controller;
    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/main.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add("/ui/main.css");
        controller = loader.getController();

        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("ZONT's Server Tools for A3");
        primaryStage.setScene(scene);
        primaryStage.show();

        controller.bt_me.setOnAction(openTool(new MEStage()));
        controller.bt_mp.setOnAction(openTool(new MPStage()));
    }

    private EventHandler<ActionEvent> openTool(Stage stage) {
        return event -> {
            try {
                stage.show();
                if (controller.cb_close.isSelected())
                    primaryStage.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
}
