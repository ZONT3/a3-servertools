package ru.zont.musicpacker.ui;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.zont.apptools.Commons;
import ru.zont.musicpacker.MPMain;
import ru.zont.musicpacker.MusicEntry;
import ru.zont.musicpacker.MusicList;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static ru.zont.apptools.Strings.STR;
import static ru.zont.apptools.Strings.normalizeString;

public class MPStage extends Stage {

    private final Parent root;
    private final Scene scene;
    private final Controller controller;
    private MPMain main;

    private final SimpleBooleanProperty nameValid = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty listValid = new SimpleBooleanProperty(false);
    private MusicList list;

    public MPStage() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/mp.fxml"));
        root = loader.load();
        scene = new Scene(root);
        scene.getStylesheets().add("/ui/main.css");
        controller = loader.getController();

        setupListeners();
        setupControls();

        setScene(scene);
        setTitle("ZONT's MusicPacker");
    }

    private void setupControls() {
        controller.grp_operations.setDisable(true);
    }

    private void setupListeners() {
        controller.bt_select.setOnAction(this::selectZip);
        controller.bt_select_f.setOnAction(this::selectFolder);
        controller.bt_dir.setOnAction(this::exportDir);
        controller.bt_zip.setOnAction(this::exportZip);
        controller.bt_cfg_m.setOnAction(this::cfgMusic);
        controller.bt_cfg_mc.setOnAction(this::cfgMusicClasses);
        controller.tf_name.textProperty().addListener(this::nameInvalidated);

        nameValid.addListener(observable -> validationStateChanged());
        listValid.addListener(observable -> validationStateChanged());
    }

    private void validationStateChanged() {
        boolean value = nameValid.get() && listValid.get();
        boolean working = main != null && main.isWorking();

        controller.grp_operations.setDisable(!value || working);
        controller.bt_select.setDefaultButton(listValid.get() && !working);
        controller.bt_select_f.setDefaultButton(listValid.get() && !working);
        controller.bt_dir.setDefaultButton(value);
        controller.bt_zip.setDefaultButton(value);
        controller.grp_top.setDisable(working);
    }

    private void nameInvalidated(Observable observable) {
        nameValid.set(!controller.tf_name.getText().isEmpty());
    }

    private void exportDir(ActionEvent event) {
        if (tableInvalid()) return;
        Commons.wrapErrors(() -> {
            DirectoryChooser fc = new DirectoryChooser();
            fc.setTitle(STR.getString("mp.fc.save.f.title"));
            File out = fc.showDialog(this);
            if (out == null) return;
            main.exportDir(out);
            listValid.set(false);
            controller.table.getItems().clear();
        });
    }

    private void exportZip(ActionEvent event) {
        if (tableInvalid()) return;
        Commons.wrapErrors(() -> {
            FileChooser fc = new FileChooser();
            fc.setTitle(STR.getString("mp.fc.save.zip.title"));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(STR.getString("mp.fc.zip"), "*.zip"));
            File out = fc.showSaveDialog(this);
            if (out == null) return;
            main.export(out);
        });
    }

    private boolean tableInvalid() {
        ObservableList<MusicEntry> items = controller.table.getItems();
        if (items.size() <= 0) {
            Toolkit.getDefaultToolkit().beep();
            new Alert(Alert.AlertType.ERROR, STR.getString("mp.err.empty")).show();
            return true;
        }
        for (MusicEntry item: items) {
            for (String x: Arrays.asList(item.getArtist(), item.getName())) {
                if (normalizeString(x)
                        .replaceAll("-", "")
                        .replaceAll("_", "")
                        .isEmpty()) {
                    Toolkit.getDefaultToolkit().beep();
                    new Alert(Alert.AlertType.ERROR, STR.getString("mp.err.field")).show();
                    return true;
                }
            }
        }
        return false;
    }

    private void selectZip(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle(STR.getString("mp.fc.open.zip.title"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(STR.getString("mp.fc.zip"), "*.zip"));
        tryParse(fc.showOpenDialog(this));
    }

    private void selectFolder(ActionEvent event) {
        DirectoryChooser fc = new DirectoryChooser();
        fc.setTitle(STR.getString("mp.fc.open.f.title"));
        tryParse(fc.showDialog(this));
    }

    private void tryParse(File file) {
        if (file == null) return;

        listValid.set(false);
        if (main != null)
            main.destruct();
        new Thread(() -> {
            try {
                main = new MPMain();
                main.workingProperty().addListener(prop -> validationStateChanged());
                MPMain.ParseResult result = main.parseList(controller.tf_name.getText(), file,
                        progress -> Platform.runLater(() -> controller.pb
                                .setProgress((double) progress.getKey() / progress.getValue())));
                controller.pb.setProgress(1.0);

                boolean valid = result.list != null && result.list.size() > 0;
                Platform.runLater(() -> {
                    if (result.errors.size() > 0) {
                        StringBuilder stringBuilder = new StringBuilder();
                        for (String error: result.errors) stringBuilder.append(error).append("\n");
                        Commons.textAreaDialog(STR.getString("mp.err.some.title"),
                                STR.getString(valid
                                        ? "mp.err.some" : "mp.err.all"),
                                stringBuilder.toString(),
                                Alert.AlertType.WARNING);
                    }
                });
                if (valid) {
                    controller.table.setItems(new ObservableListWrapper<>(result.list));
                    controller.tf_name.textProperty().bindBidirectional(result.list.displayNameProperty());
                    listValid.set(true);
                }
            } catch (Throwable t) {
                Platform.runLater(() -> Commons.reportError("Error parsing list", t));
            }
        }, "MPMain").start();
    }

    private void copyCfg(String content, String cfgName) {
        if (controller.cb_wrap.isSelected())
            content = String.format("class %s {\n%s\n};", cfgName, content);
        StringSelection contents = new StringSelection(content);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, contents);
    }

    private void cfgMusic(ActionEvent event) {
        copyCfg(main.buildCfgMusic(controller.tf_prefix.getText()), "CfgMusic");
    }

    private void cfgMusicClasses(ActionEvent event) {
        copyCfg(main.buildCfgMusicClasses(), "CfgMusicClasses");
    }

    @Override
    public void close() {
        System.out.println("MP Closed!");
        if (main != null)
            main.destruct();
    }
}
