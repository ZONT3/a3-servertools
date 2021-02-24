package ru.zont.apptools;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import java.io.IOException;

public class ErrorDialog extends Dialog<Void> {

    public ErrorDialog(String title, String content) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/error.fxml"));
            Parent root = loader.load();
            ErrorDialogController controller = loader.getController();
            getDialogPane().setContent(root);

            getDialogPane().getButtonTypes().add(ButtonType.OK);
            getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            Node closeButton = getDialogPane().lookupButton(ButtonType.CLOSE);
            closeButton.managedProperty().bind(closeButton.visibleProperty());
            closeButton.setVisible(false);

            controller.title.setText(title);
            controller.content.setText(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
