package ru.zont.servertools;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.zont.modsextractor.ui.MEStage;

import java.io.IOException;

public class AppMain extends Application {

    private MainController controller;

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

        primaryStage.setTitle("ZONT's Server Tools for A3");
        primaryStage.setScene(scene);
        primaryStage.show();

        controller.bt_me.setOnAction(event -> {
            try {
                new MEStage().show();
                if (controller.cb_close.isSelected())
                    primaryStage.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
