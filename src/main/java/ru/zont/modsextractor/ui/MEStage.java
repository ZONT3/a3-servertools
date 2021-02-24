package ru.zont.modsextractor.ui;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.zont.Commons;
import ru.zont.modsextractor.Mod;
import ru.zont.modsextractor.Parser;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.util.ArrayList;

public class MEStage extends Stage {

    private final Scene scene;
    private final Parent root;
    private final MEController controller;

    public MEStage() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/me.fxml"));
        root = loader.load();
        scene = new Scene(root);
        controller = loader.getController();

        addListeners();

        setScene(scene);
        setTitle("ZONT's ModsExtractor");
    }

    private void addListeners() {
        controller.bt_ap.setOnAction(this::getAP);
        controller.bt_us.setOnAction(this::getUS);
        controller.bt_select.setOnAction(this::openFileChooser);
    }

    private void openFileChooser(ActionEvent event) {
        FileChooser fc = new FileChooser();

        File prev = getPrev(new File(getAppData(), "ModsExtractorData"));
        if (prev != null && prev.isFile()) {
            fc.setInitialDirectory(prev.getParentFile());
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arma 3 Preset", "*.html"));
            fc.setInitialFileName(prev.getName());
        }

        File file = fc.showOpenDialog(this);
        if (file != null) parsePreset(file);
    }

    private void parsePreset(File file) {
        try {
            ArrayList<Mod> list = Parser.parse(file);
            controller.table.getItems().addAll(list);
        } catch (Exception e) {
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
