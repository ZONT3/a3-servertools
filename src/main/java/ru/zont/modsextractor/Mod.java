package ru.zont.modsextractor;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import ru.zont.apptools.Commons;

import java.io.File;
import java.util.List;

public class Mod {
    private final SimpleStringProperty name;
    private final SimpleLongProperty id;
    private final SimpleStringProperty link;

    public Mod(String name, long id) {
        this.name = new SimpleStringProperty(name);
        this.id = new SimpleLongProperty(id);
        link = new SimpleStringProperty("");
    }

    public String getName() {
        return name.get();
    }

    public long getId() {
        return id.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public void setId(long id) {
        this.id.set(id);
    }

    public String getLink() {
        return link.get();
    }

    public void setLink(String link) {
        this.link.set(link);
    }

    public SimpleLongProperty idProperty() {
        return id;
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public SimpleStringProperty linkProperty() {
        return link;
    }

    public long modSize(File workshopDir) {
        File thisDir = new File(workshopDir, "107410");
        if (!thisDir.isDirectory()) throw new IllegalArgumentException("Corrupted workshop dir or mod is absent");
        return Commons.size(thisDir.toPath());
    }

    public static long modsSize(List<Mod> list, File workshopDir) {
        long sum = 0;
        try {
            for (Mod mod: list) sum += mod.modSize(workshopDir);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return -1;
        }
        return sum;
    }
}
