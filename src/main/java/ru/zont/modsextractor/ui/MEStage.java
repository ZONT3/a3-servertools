package ru.zont.modsextractor.ui;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
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
import ru.zont.modsextractor.Mod;
import ru.zont.modsextractor.ModList;
import ru.zont.modsextractor.Parser;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.util.Arrays;
import java.util.List;

public class MEStage extends Stage {

    private final Scene scene;
    private final Parent root;
    private final MEController controller;

    private final SimpleStringProperty workshopDir = new SimpleStringProperty();
    private final SimpleObjectProperty<File> presetFile = new SimpleObjectProperty<>(null);
    private ModList modList = null;

    public MEStage() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/me.fxml"));
        root = loader.load();
        scene = new Scene(root);
        scene.getStylesheets().add("/ui/main.css");
        scene.getStylesheets().add("/ui/me.css");
        controller = loader.getController();

        addListeners();
        tryFindWorkshop();
        setupControls();

        setScene(scene);
        setTitle("ZONT's ModsExtractor");
    }

    private void setupControls() {
        controller.bt_ap.setDisable(true);
        controller.bt_us.setDisable(true);
        controller.bt_save.setDisable(true);
    }

    private void tryFindWorkshop() {
        List<String> dirs = Arrays.asList(
            "/Program Files (x86)/Steam/steamapps/workshop/content",
            "/Program Files/Steam/steamapps/workshop/content",
            "SteamLibrary/steamapps/workshop/content"
        );

        for (File root: File.listRoots()) {
            for (String dir: dirs) {
                File file = new File(root, dir);
                if (file.isDirectory() && isValidWorkshopDir(file)) {
                    workshopDir.setValue(file.getAbsolutePath());
                    return;
                }
            }
        }
        workshopDir.setValue("");
    }

    private void addListeners() {
        controller.bt_ap.setOnAction(this::getAP);
        controller.bt_us.setOnAction(this::getUS);
        controller.bt_select.setOnAction(this::selectPreset);
        controller.bt_ws.setOnAction(this::selectWorkshop);
        controller.bt_save.setOnAction(this::save);

        workshopDir.addListener(this::workshopDirInvalidated);
        presetFile.addListener(this::presetInvalidated);
    }

    private void presetInvalidated(ObservableValue<? extends File> observableValue, File was, File cur) {
        boolean value = cur == null;
        controller.bt_ap.setDisable(value);
        controller.bt_us.setDisable(value);
        controller.bt_save.setDisable(value);
    }

    private void save(ActionEvent event) {
        writePreset(modList, presetFile.get());
    }

    private void workshopDirInvalidated(ObservableValue<? extends String> observableValue, String prev, String cur) {
        Commons.setValid(controller.lb_ws, !cur.isEmpty() && isValidWorkshopDir(new File(cur)));
    }

    private void selectWorkshop(ActionEvent event) {
        DirectoryChooser fc = new DirectoryChooser();
        fc.setTitle("Select steamapps/workshop/content dir");
        File file = fc.showDialog(this);

        if (file == null || !file.isDirectory()) return;

        try {
            if (isValidWorkshopDir(file))
                workshopDir.setValue(file.getAbsolutePath());
            else new Alert(Alert.AlertType.ERROR,
                    "Specified directory is not " +
                            "a workshop content directory, " +
                            "or it does not contain a 107410 subdir")
                    .show();
        } catch (Throwable e) {
            Commons.reportError(null, e);
        }
    }

    private boolean isValidWorkshopDir(File file) {
        File[] files = file.listFiles(File::isDirectory);
        if (files == null) throw new NullPointerException("files");
        for (File f: files)
            if ("107410".equals(f.getName()) && f.isDirectory())
                return true;
        return false;
    }

    private void selectPreset(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arma 3 Preset", "*.html"));

        File prev = getPrev(new File(getAppData(), "ModsExtractorData"));
        if (prev != null && prev.isFile()) {
            fc.setInitialDirectory(prev.getParentFile());
            fc.setInitialFileName(prev.getName());
        }

        presetFile.set(fc.showOpenDialog(this));
        if (presetFile.get() != null) applyPreset();
    }

    private void applyPreset() {
        try {
            modList = Parser.parse(presetFile.get());
            controller.table.getItems().addAll(modList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writePreset(ModList parse, File file) {
        try {
            PrintWriter pw = new PrintWriter(new FileOutputStream(file));
            pw.write(parse.getPresetHTML());
            pw.flush();
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void getUS(ActionEvent event) {
        ObservableList<Mod> mods = controller.table.getItems();

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Mod mod: mods) {
            if (first) first = false;
            else sb.append(",");
            sb.append("\n").append(String.format("    %-50s \"%d\"",
                            String.format("\"%s\":", mod.getLink()),
                            mod.getId()));
        }

        copy(sb);
    }

    private void getAP(ActionEvent event) {
        ObservableList<Mod> mods = controller.table.getItems();
        String prefix = controller.tf_prefix.getText();
        File file = new File(prefix);

        StringBuilder sb = new StringBuilder("-mod=");
        String separator;
        if (!Commons.isWindows()) separator = "\\;";
        else separator = ";";
        for (Mod mod: mods) {
            sb.append(file.getPath())
                    .append(controller.sp_sep.getValue())
                    .append(mod.getLink())
                    .append(separator);
        }

        copy(sb);
    }

    private void copy(CharSequence str) {
        StringSelection selection = new StringSelection(str.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    private static String getAppData() {
        String workingDirectory;
        String OS = (System.getProperty("os.name")).toUpperCase();
        if (OS.contains("WIN")) {
            workingDirectory = System.getenv("AppData");
            workingDirectory += "/modsextr";
        } else {
            workingDirectory = System.getProperty("user.home");
            workingDirectory += "/.modsextr";
        }
        return workingDirectory;
    }

    private static File getPrev(File f) {
        f.getParentFile().mkdirs();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            String s = (String) ois.readObject();
            return new File(s);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void storePrev(File f, File prev) {
        f.delete();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            oos.writeObject(prev.getAbsolutePath());
            oos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
