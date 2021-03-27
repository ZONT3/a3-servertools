package ru.zont.modsextractor.ui;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
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
import ru.zont.apptools.Strings;
import ru.zont.modsextractor.Mod;
import ru.zont.modsextractor.ModList;
import ru.zont.modsextractor.Parser;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MEStage extends Stage {
    private final Scene scene;
    private final Parent root;
    private final Controller controller;

    private final SimpleBooleanProperty loadingCompare = new SimpleBooleanProperty(false);
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
        controller.grp_operations.setDisable(true);
        controller.grp_info.setDisable(true);
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
        controller.bt_ws.setOnAction(this::selectWorkshop);
        controller.bt_save.setOnAction(this::save);
        controller.bt_select.setOnAction(event -> {
            presetFile.set(selectPreset());
            if (presetFile.get() != null) applyPreset();
        });
        controller.bt_compare.setOnAction(event -> {
            File file = selectPreset();
            new Thread(() -> {
                if (file != null) comparePreset(file);
            }, "Comparing presets").start();
        });
        controller.bt_info.setOnAction(event -> {
            Commons.textAreaDialog("Modpack info", null,
                    getModpackTotalStr(modList, new File(workshopDir.get())) + "\n",
                    Alert.AlertType.INFORMATION).show();
        });

        workshopDir.addListener(this::workshopDirInvalidated);
        presetFile.addListener(this::presetInvalidated);
        loadingCompare.addListener(this::presetInvalidated);
    }

    private void presetInvalidated(Observable observableValue) {
        boolean value = !isValidArma3Preset(presetFile.get()) || loadingCompare.get();
        controller.grp_operations.setDisable(value);
        controller.grp_info.setDisable(value || !isValidWorkshopDir(workshopDir.get()));
    }

    private void save(ActionEvent event) {
        writePreset(modList, presetFile.get());
    }

    private void workshopDirInvalidated(ObservableValue<? extends String> observableValue, String prev, String cur) {
        boolean valid = isValidWorkshopDir(cur);
        Commons.setValid(controller.lb_ws, valid);
        controller.grp_info.setDisable(!valid || presetFile.get() == null);
    }

    private boolean isValidArma3Preset(File cur) {
        return cur != null && cur.isFile() && cur.getName().endsWith(".html");
    }

    private boolean isValidWorkshopDir(String cur) {
        return !cur.isEmpty() && isValidWorkshopDir(new File(cur));
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

    private File selectPreset() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arma 3 Preset", "*.html"));

        File prev = getPrev(new File(getAppData(), "ModsExtractorData"));
        if (prev != null && prev.isFile()) {
            fc.setInitialDirectory(prev.getParentFile());
            fc.setInitialFileName(prev.getName());
        }

        return fc.showOpenDialog(this);
    }

    private void applyPreset() {
        try {
            modList = Parser.parse(presetFile.get());
            controller.table.getItems().clear();
            controller.table.getItems().addAll(modList);
        } catch (Exception e) {
            Commons.reportError("Error", e);
        }
    }

    private void comparePreset(File file) {
        try {
            ModList comparing = Parser.parse(file, false);
            ArrayList<Mod> additional = comparing.getAdditionalMods(modList);
            ArrayList<Mod> subtracted = modList.getAdditionalMods(comparing);

            File workshopDir = new File(this.workshopDir.get());

            long additionalSize = Mod.modsSize(additional, workshopDir);
            long subtractedSize = Mod.modsSize(subtracted, workshopDir);


            String header;
            if (additionalSize < 0 || subtractedSize < 0)
                header = "Some of sizes cannot be computed, because some mods are absent in your workshop dir.";
            else header = null;

            StringBuilder sizeStr = new StringBuilder();
            if (additionalSize > 0 || subtractedSize > 0) {
                if (subtractedSize > 0)
                    sizeStr.append(String.format("-%02.02fGB", toGB(subtractedSize)));
                if (additionalSize > 0)
                    sizeStr.append(sizeStr.length() == 0 ? "" : " ")
                            .append(String.format("+%02.02fGB", toGB(additionalSize)));
            } else sizeStr.append("???");

            StringBuilder modsStr = new StringBuilder();
            int sSize = subtracted.size();
            int aSize = additional.size();
            if (sSize > 0) modsStr.append(String.format("-%d ", sSize));
            if (aSize > 0) modsStr.append(String.format("+%d ", aSize));
            modsStr.append(Strings.getPlural(aSize > 0 ? aSize : sSize, "plurals.mods"));

            String content = String.format("%s\n\n%s\n%s",
                    Strings.STR.getString("mods.update.title"),
                    Strings.STR.getString("mods.update.size", sizeStr, modsStr),
                    getModpackTotalStr(comparing, workshopDir));

            Platform.runLater(() ->
                    Commons.textAreaDialog("Modpack update template",
                            header, content, Alert.AlertType.INFORMATION)
                    .show());
        } catch (IOException e) {
            Commons.reportError("Error", e);
        }
    }

    private String getModpackTotalStr(ModList comparing, File workshopDir) {
        long total = Mod.modsSize(comparing, workshopDir);
        String totalStr = total > 0 ? String.format("%02.02fGB", toGB(total)) : "???";
        return Strings.STR.getString("mods.info.size", totalStr, String.format("%d %s", comparing.size(), Strings.getPlural(comparing.size(), "plurals.mods")));
    }

    private float toGB(long b) {
        return b / 1000.f / 1000.f / 1000.f;
    }

    private static void writePreset(ModList parse, File file) {
        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
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
